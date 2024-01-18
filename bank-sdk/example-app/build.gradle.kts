import net.gini.gradle.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

// TODO: construct version code and name in fastlane and inject them
//apply from: rootProject.file("gradle/git_utils.gradle")
//apply from: rootProject.file("gradle/gini_credentials.gradle")
//
//def appVersionCode = gitCommitUnixTime()
//def appVersionName = "${version}-${gitBranch()}-${gitHash()} (${appVersionCode})"
//
//task printVersion {
//    doLast {
//        println "${appVersionName}"
//    }
//}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml
    namespace = "net.gini.android.bank.sdk.exampleapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // after upgrading to AGP 8, we need this to have the defaultConfig block
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "net.gini.android.bank.sdk.exampleapp"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionName = version as String
        versionCode = (properties["versionCode"] as? String)?.toInt() ?: 0

        // Use the test runner with JUnit4 support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(properties["releaseKeystoreFile"] ?: "")
            storePassword = (properties["releaseKeystorePassword"] as? String) ?: ""
            keyAlias = (properties["releaseKeyAlias"] as? String) ?: ""
            keyPassword = (properties["releaseKeyPassword"] as? String) ?: ""
        }
    }

    buildTypes {
        val credentials = readLocalPropertiesToMapSilent(project, listOf("clientId", "clientSecret"))

        debug {
            resValue("string", "gini_api_client_id", credentials["clientId"] ?: "")
            resValue("string", "gini_api_client_secret", credentials["clientSecret"] ?: "")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")

            resValue("string", "gini_api_client_id", credentials["clientId"] ?: "")
            resValue("string", "gini_api_client_secret", credentials["clientSecret"] ?: "")
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

// after upgrading to AGP 8, we need this, otherwise, gradle will complain to use the same jdk version as your machine (17 which is bundled with Android Studio)
// https://youtrack.jetbrains.com/issue/KT-55947/Unable-to-set-kapt-jvm-target-version
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    // For testing the local version
    implementation(project(":capture-sdk:sdk"))
    // For testing a released version
    //implementation "net.gini.android:gini-capture-sdk:0.0.1"

    // For testing the local version
    implementation(project(":capture-sdk:default-network"))
    // For testing a released version
    //implementation "net.gini.android:gini-capture-sdk-default-network:0.0.1"

    // For testing the local version
    implementation(project(":bank-sdk:sdk"))
    // For testing a released version
    //implementation "net.gini.android:gini-bank-sdk:3.3.0"

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.multidex)
    implementation(libs.dexter)
    implementation(libs.logback.android.core)
    implementation(libs.logback.android.classic) {
        // workaround issue #73
        exclude(group = "com.google.android", module = "android")
    }

    implementation(libs.lottie)

    implementation(libs.hilt.library)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.test.espresso.idling.resource)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.androidx.test.junit)
}

// this is needed because of Dagger-Hilt
kapt {
    correctErrorTypes = true
}
apply<CodeAnalysisPlugin>()
