import net.gini.gradle.*
import org.jetbrains.dokka.gradle.DokkaCollectorTask

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("jacoco")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.bank.sdk"
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
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", "\"$version\"")
        buildConfigField("String", "VERSION_CODE", "\"${properties["versionCode"]}\"")
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        debug {
            // Disabled due to jacoco throwing an exception: "Unexpected SMAP line: *S KotlinDebug"
            // This workaround didn't help either: https://youtrack.jetbrains.com/issue/KT-44757#focus=Comments-27-5247441.0-0
            isTestCoverageEnabled = false
        }
        release {
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
        // required to prevent .kotlin_module file name collisions
        // https://stackoverflow.com/a/56329676/276129
        moduleName = "${properties["groupId"]}.${properties["artifactId"]}"
    }
}

// after upgrading to AGP 8, we need this, otherwise, gradle will complain to use the same jdk version as your machine (17 which is bundled with Android Studio)
// https://youtrack.jetbrains.com/issue/KT-55947/Unable-to-set-kapt-jvm-target-version
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {

    api(project(":bank-api-library:library"))
    api(project(":capture-sdk:sdk"))
    api(project(":capture-sdk:default-network"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.jUnitParams)

    androidTestImplementation(libs.moshi.core)
    kaptAndroidTest(libs.moshi.codegen)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}


apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Bank SDK for Android")
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
