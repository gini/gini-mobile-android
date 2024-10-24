import net.gini.gradle.*
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
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

fun loadPaymentProviderApps(): List<Map<String, String>> {
    fun parsePaymentProvidersToml(tomlParseResult: TomlParseResult): List<Map<String, String>> =
        tomlParseResult.getArray("paymentProviderApps")?.toList()?.map { (it as TomlTable).toMap() as Map<String, String> }
            ?: emptyList()

    // Load test payment providers (these can be checked into git)
    val testPaymentProviderApps: List<Map<String, String>> =
        parsePaymentProvidersToml(Toml.parse(project.file("paymentProviderApps/testPaymentProviderApps.toml").readText()))

    // Load mock payment providers (these MUST NOT be checked into git because they contain sensitive data)
    val mockPaymentProviderApps: List<Map<String, String>> = try {
        parsePaymentProvidersToml(Toml.parse(project.file("paymentProviderApps/mockPaymentProviderApps.toml").readText()))
    } catch (e: Exception) {
        emptyList()
    }

    return testPaymentProviderApps + mockPaymentProviderApps
}

val paymentProviderApps = loadPaymentProviderApps()

tasks.create("printNrOfTestPaymentProviders") {
    doLast {
        println(paymentProviderApps.size)
    }
}

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
    flavorDimensions += listOf("environment", "purpose")
    productFlavors {
        create("prod") {
            dimension = "environment"
        }
        create("dev") {
            isDefault = true
            dimension = "environment"
        }
        create("qa") {
            dimension = "environment"
        }
        create("exampleApp") {
            isDefault = true
            dimension = "purpose"
            resValue("string", "gini_pay_connect_host", "")
            resValue("string", "gini_pay_connect_scheme", "")
        }
        paymentProviderApps.forEachIndexed { i, paymentProvider ->
            create("paymentProvider${i+1}") {
                dimension = "purpose"
                applicationId = paymentProvider["applicationId"] as String
                resValue("string", "app_name", paymentProvider["appName"] as String)
                resValue("string", "gini_pay_connect_host", "payment")
                resValue("string", "gini_pay_connect_scheme", "ginipay")
            }
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    sourceSets {
        getByName("debug") {
            assets.srcDirs("${projectDir}/src/androidTest/assets")
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.test.espresso.idlingresource)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.multidex)
    androidTestImplementation(libs.androidx.test.junit)

    androidTestUtil(libs.androidx.test.orchestrator)
}

// this is needed because of Dagger-Hilt
kapt {
    correctErrorTypes = true
}
apply<CodeAnalysisPlugin>()
