import net.gini.gradle.Versions

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = Versions.Android.compileSdk

    defaultConfig {
        applicationId = "net.gini.pay.app"
        minSdk = Versions.Android.minSdk
        targetSdk = Versions.Android.targetSdk
        versionCode = 1
        versionName ="1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
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

    implementation(project(":health-sdk:sdk"))

    implementation("androidx.core:core-ktx:${Versions.Deps.androidXCore}")
    implementation("androidx.activity:activity-ktx:${Versions.Deps.activity}")
    implementation("androidx.fragment:fragment-ktx:${Versions.Deps.fragment}")
    implementation("androidx.appcompat:appcompat:${Versions.Deps.appCompat}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.Deps.androidXLifecycle}")
    implementation("com.google.android.material:material:${Versions.Deps.material}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.Deps.constraintLayout}")
    val koinVersion = "2.2.2"
    implementation("io.insert-koin:koin-androidx-scope:$koinVersion")
    implementation("io.insert-koin:koin-androidx-viewmodel:$koinVersion")
    implementation("io.insert-koin:koin-androidx-fragment:$koinVersion")
    implementation("dev.chrisbanes.insetter:insetter:0.5.0")

    testImplementation("junit:junit:${Versions.Test.jUnit}")

    androidTestImplementation("androidx.test.ext:junit:${Versions.Test.androidXTestJUnit}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.Test.espresso}")
}