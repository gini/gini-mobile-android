plugins {
    id("com.android.library")
    kotlin("android")
}


android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    // For testing the local version
    api(project(":capture-sdk:sdk"))
    // For testing a released version
    //implementation "net.gini.android:gini-capture-sdk:0.0.1"

    // For testing the local version
    api(project(":capture-sdk:default-network"))
    // For testing a released version
    //implementation "net.gini.android:gini-capture-sdk-default-network:0.0.1"

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.multidex)
    implementation(libs.dexter)
}
