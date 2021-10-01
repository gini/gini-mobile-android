import net.gini.gradle.DokkaPlugin
import net.gini.gradle.MavenPublishPlugin
import net.gini.gradle.Versions
import java.net.URI

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
}

android {
    compileSdk = Versions.Android.compileSdk

    defaultConfig {
        minSdk = Versions.Android.minSdk
        targetSdk = Versions.Android.targetSdk

        // Use the test runner with JUnit4 support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        consumerProguardFiles "consumer-rules.pro"

        // TODO: inject version code and name
//        buildConfigField("int', 'VERSION_CODE', "${rootProject.ext.versionCode}"
//        buildConfigField("String', 'VERSION_NAME', "\"${rootProject.ext.versionName}\""
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = true
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

    api(project(":health-api-library:library"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Deps.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.Deps.coroutines}")
    implementation("androidx.lifecycle:lifecycle-common-java8:${Versions.Deps.androidXLifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.Deps.androidXLifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.Deps.androidXLifecycle}")
    implementation("com.google.android.material:material:${Versions.Deps.material}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.Deps.constraintLayout}")
    implementation("androidx.fragment:fragment-ktx:${Versions.Deps.fragment}")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("dev.chrisbanes.insetter:insetter:0.5.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.Deps.coroutines}")
    testImplementation("app.cash.turbine:turbine:0.4.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

apply<MavenPublishPlugin>()
apply<DokkaPlugin>()

// TODO: how to modernize code quality checks?
// apply from: rootProject.file('gradle/codequality.gradle')

