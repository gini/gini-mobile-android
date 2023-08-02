import net.gini.gradle.*
import org.jetbrains.dokka.gradle.DokkaCollectorTask

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("jacoco")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.health.api"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // after upgrading to AGP 8, we need this to have the defaultConfig block
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Use the test runner with JUnit4 support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VERSION_NAME", "\"$version\"")
        buildConfigField("String", "VERSION_CODE", "\"${properties["versionCode"]}\"")
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
            consumerProguardFile("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
        // required to prevent .kotlin_module file name collisions
        // https://stackoverflow.com/a/56329676/276129
        moduleName = "${properties["groupId"]}.${properties["artifactId"]}"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// after upgrading to AGP 8, we need this, otherwise, gradle will complain to use the same jdk version as your machine (17 which is bundled with Android Studio)
// https://youtrack.jetbrains.com/issue/KT-55947/Unable-to-set-kapt-jvm-target-version
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    api(project(":core-api-library:library"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.moshi.core)
    kapt(libs.moshi.codegen)

    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(project(":core-api-library:shared-tests"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
}

apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()
apply<PropertiesPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Health API Library for Android")
}

tasks.register<CreatePropertiesTask>("injectTestProperties") {
    val propertiesMap = mutableMapOf<String, String>()

    doFirst {
        propertiesMap.clear()
        propertiesMap.putAll(readLocalPropertiesToMap(project,
            listOf("testClientId", "testClientSecret", "testApiUri", "testUserCenterUri")))
    }

    destinations.put(
        file("src/androidTest/assets/test.properties"),
        propertiesMap
    )
}

afterEvaluate {
    tasks.filter { it.name.endsWith("androidTest", ignoreCase = true) }.forEach {
        it.dependsOn(tasks.getByName("injectTestProperties"))
    }
}
