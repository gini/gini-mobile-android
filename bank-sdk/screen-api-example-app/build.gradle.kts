import net.gini.gradle.*

plugins {
    id("com.android.application")
    kotlin("android")
}

// TODO: construct version code and name in fastlane and inject them

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // TODO: change it once we update our example payment provider: "net.gini.android.bank.sdk.screenapiexample"
        applicationId = "net.gini.pay.appscreenapi"

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionName = version as String
        versionCode = (properties["versionCode"] as? String)?.toInt() ?: 0

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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

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

dependencies {
    implementation(project(":bank-sdk:sdk"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.koin.androidx.scope)
    implementation(libs.koin.androidx.viewmodel)
    implementation(libs.koin.androidx.fragment)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}