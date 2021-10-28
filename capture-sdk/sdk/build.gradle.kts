import net.gini.gradle.*
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    kotlin("android")
    id("com.hiya.jacoco-android")
}


jacoco {
    toolVersion = "0.8.7"
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Use the test runner with JUnit4 support
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

    // TODO: is this still needed?
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
    }
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
    implementation(libs.playservices.vision)
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

// TODO: is this needed?
//apply from: rootProject.file("gradle/multidex_for_tests.gradle")

apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()

tasks.getByName<DokkaTask>("dokkaHtml") {
    dokkaSourceSets.named("main") {
        perPackageOption {
            matchingRegex.set("""com\.ortiz.*""")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set("""net\.gini\.android\.capture\.internal""")
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
