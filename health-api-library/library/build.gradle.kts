import net.gini.gradle.*

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = Versions.Android.compileSdk

    defaultConfig {
        minSdk = Versions.Android.minSdk
        targetSdk = Versions.Android.targetSdk

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
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.core:core-ktx:${Versions.Deps.androidXCore}")

    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Versions.Deps.moshi}")
    implementation("com.squareup.moshi:moshi:${Versions.Deps.moshi}")

    // Mocks for testing.
    androidTestImplementation("org.mockito:mockito-core:${Versions.Test.mockito}")
    androidTestImplementation("org.mockito:mockito-android:${Versions.Test.mockito}")
    androidTestImplementation("androidx.test:runner:${Versions.Test.androidXTest}")
    androidTestImplementation("androidx.test:rules:${Versions.Test.androidXTest}")
    androidTestImplementation("androidx.test.ext:junit:${Versions.Test.androidXTestJUnit}")
    androidTestImplementation("androidx.multidex:multidex:${Versions.Deps.androidXMultiDex}")
}

apply<MavenPublishPlugin>()
apply<DokkaPlugin>()
apply<TestPropertiesPlugin>()

//// TODO: remove this?
//apply from: rootProject.file('gradle/javadoc_coverage.gradle')