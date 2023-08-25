import net.gini.gradle.*
import org.jetbrains.dokka.gradle.DokkaCollectorTask
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    id("jacoco")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.capture"
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
        debug {
            isTestCoverageEnabled = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    packagingOptions {
        resources {
            // Fix for DuplicateFileException when using Espresso (https://code.google.com/p/android/issues/detail?id=195331)
            excludes.add("META-INF/maven/com.google.guava/guava/pom.properties")
            excludes.add("META-INF/maven/com.google.guava/guava/pom.xml")

            // Fix for androidTest builds due to Play Services Vision and Espresso creating their own protobuf.meta files
            pickFirsts.add("protobuf.meta")
        }
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

    sourceSets {
        getByName("debug") {
            res.srcDirs("${projectDir}/src/test/res")
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
    api(libs.slf4j.api)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.androidx.cardview)
    implementation(libs.mlkit.barcodescanning)
    implementation(libs.apachecommons.imaging)
    implementation(libs.completableFuture)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin2)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.espresso.intents)
    testImplementation(libs.jUnitParams)

    debugImplementation(libs.androidx.test.core.ktx)
    debugImplementation(libs.androidx.fragment.testing)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.multidex)

    androidTestUtil(libs.androidx.test.orchestrator)
}

apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Capture SDK for Android")
}

tasks.getByName<DokkaTask>("dokkaHtml") {
    dokkaSourceSets.named("main") {
        perPackageOption {
            matchingRegex.set("""com\.ortiz.*""")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set("""net\.gini\.android\.capture\.internal.*""")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set("""net\.gini\.android\.capture\.util""")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set("""net\.gini\.android\.capture\.review\.multipage\.previews""")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set("""net\.gini\.android\.capture\.review\.multipage\.thumbnails""")
            suppress.set(true)
        }
    }
}
