import net.gini.gradle.*
import net.gini.gradle.extensions.apiProjectDependencyForSBOM
import org.jetbrains.dokka.gradle.DokkaCollectorTask

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.health.sdk"
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

        // Setting 'vectorDrawables.useSupportLibrary = true' prevents creating PNGs for vector drawables.
        // This is needed to allow overriding of drawables either with other vector drawables added to 'drawable-anydpi'
        // or with PNGs.
        // If 'vectorDrawables.useSupportLibrary' is not set to 'true' then PNGs won't override vector drawables and
        // overriding of vector drawables with other vector drawables only works on API Level 24 and higher if they
        // are also added to 'drawable-anydpi-v24' in addition to 'drawable-anydpi'.
        // Our min SDK level allows only Android versions which have runtime support for vector drawables
        // so it's safe to prevent creating PNGs from vector drawables and we also reduce the apk size.
        // TODO: when minSDK is raised to 24 or above delete PNGs for which we have vector drawables.
        //       Background: for API Levels 21 to 23 we had to add PNGs for some vector drawables because
        //       they were not rendered correctly.
        vectorDrawables.useSupportLibrary = true
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
        // required to prevent .kotlin_module file name collisions
        // https://stackoverflow.com/a/56329676/276129
        moduleName = "${properties["groupId"]}.${properties["artifactId"]}"
        // Fix for "Inheritance from an interface with '@JvmDefault' members is only allowed with -Xjvm-default option"
        // https://issuetracker.google.com/issues/217593040#comment6
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
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
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    val healthApiLibrary = project(":health-api-library:library")
    if (properties["createSBOM"] == "true") {
        apiProjectDependencyForSBOM(healthApiLibrary)
    } else {
        api(healthApiLibrary)
    }

    api(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.photoview)
    implementation(libs.insetter)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.androidx.test.core.ktx)
    debugImplementation(libs.androidx.fragment.testing)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.espresso.intents)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()
apply<SBOMPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Health SDK for Android")
}
