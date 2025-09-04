import net.gini.gradle.*
import net.gini.gradle.extensions.apiProjectDependencyForSBOM
import net.gini.gradle.extensions.implementationProjectDependencyForSBOM
import org.jetbrains.dokka.gradle.DokkaCollectorTask

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
    id("jacoco")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.capture.network"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // after upgrading to AGP 8, we need this to have the defaultConfig block
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.android.targetSdk.get().toInt()
        lint.targetSdk = libs.versions.android.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", "\"$version\"")
        buildConfigField("String", "VERSION_CODE", "\"${properties["versionCode"]}\"")
    }

    buildTypes {
        debug {
            isTestCoverageEnabled = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
        // required to prevent .kotlin_module file name collisions
        // https://stackoverflow.com/a/56329676/276129
        moduleName = "${properties["groupId"]}.${properties["artifactId"]}"
    }
    // After AGP 8, this replaces the tasks in PublishToMavenPlugin
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}
// after upgrading to AGP 8, we need this, otherwise, gradle will complain to use the same jdk version as your machine (17 which is bundled with Android Studio)
// https://youtrack.jetbrains.com/issue/KT-55947/Unable-to-set-kapt-jvm-target-version
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    api(libs.slf4j.api)

    val bankApiLibrary = project(":bank-api-library:library")
    if (properties["createSBOM"] == "true") {
        apiProjectDependencyForSBOM(bankApiLibrary)
    } else {
        api(bankApiLibrary)
    }

    val captureSdk = project(":capture-sdk:sdk")
    if (properties["createSBOM"] == "true") {
        implementationProjectDependencyForSBOM(captureSdk)
    } else {
        implementation(captureSdk)
    }

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)

    androidTestImplementation(libs.moshi.core)
    kspAndroidTest(libs.moshi.codegen)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
}

apply<PublishToMavenPlugin>()
apply<CodeAnalysisPlugin>()
apply<DokkaPlugin>()
apply<SBOMPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Capture SDK - Default Network Library for Android")
}

tasks.register<CreatePropertiesTask>("injectTestProperties") {
    val propertiesMap = mutableMapOf<String, String>()

    doFirst {
        propertiesMap.clear()
        propertiesMap.putAll(readLocalPropertiesToMapSilent(project,
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