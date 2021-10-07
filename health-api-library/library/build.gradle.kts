import net.gini.gradle.CodeAnalysisPlugin
import net.gini.gradle.DokkaPlugin
import net.gini.gradle.TestPropertiesPlugin
import net.gini.gradle.TestPropertiesPluginExtension

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Use the test runner with JUnit4 support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VERSION_NAME", "\"${version}\"")
    }
    buildTypes {
        getByName("debug") {
            // Disabled due to a jacoco error when using kotlin 1.5 (java.lang.IllegalStateException: Unexpected SMAP line: *S KotlinDebug)
            isTestCoverageEnabled = false
            // Needed for instrumented tests
            multiDexEnabled = true
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api("com.android.volley:volley:1.2.1")
    api("com.parse.bolts:bolts-android:1.4.0")
    implementation("com.datatheorem.android.trustkit:trustkit:1.1.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    kapt(libs.moshi.codegen)
    implementation(libs.moshi.core)

    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.multidex)
}

apply<MavenPublishPlugin>()
apply<DokkaPlugin>()
apply<TestPropertiesPlugin>()
apply<CodeAnalysisPlugin>()

configure<TestPropertiesPluginExtension> {
    properties.apply {
        (project.properties["testClientId"] as? String)?.let { put("testClientId", it) }
        (project.properties["testClientSecret"] as? String)?.let { put("testClientSecret", it) }
        (project.properties["testApiUri"] as? String)?.let { put("testApiUri", it) }
        (project.properties["testUserCenterUri"] as? String)?.let { put("testUserCenterUri", it) }
    }
}

// TODO: remove this?
// apply from: rootProject.file('gradle/javadoc_coverage.gradle')
