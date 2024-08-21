import net.gini.gradle.*
import net.gini.gradle.extensions.apiProjectDependencyForSBOM
import org.jetbrains.dokka.gradle.DokkaCollectorTask

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("jacoco")
    id("androidx.navigation.safeargs.kotlin")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

android {
    // after upgrading to AGP 8, we need this (copied from the module's AndroidManifest.xml)
    namespace = "net.gini.android.bank.sdk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // after upgrading to AGP 8, we need this to have the defaultConfig block
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.android.targetSdk.get().toInt()
        lint.targetSdk = libs.versions.android.targetSdk.get().toInt()

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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }

    buildTypes {
        debug {
            // Disabled due to jacoco throwing an exception: "Unexpected SMAP line: *S KotlinDebug"
            // This workaround didn't help either: https://youtrack.jetbrains.com/issue/KT-44757#focus=Comments-27-5247441.0-0
            isTestCoverageEnabled = false
        }
        release {
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

    val bankApiLibrary = project(":bank-api-library:library")
    val captureSdk = project(":capture-sdk:sdk")
    val captureSdkDefaultNetwork = project(":capture-sdk:default-network")
    if (properties["createSBOM"] == "true") {
        apiProjectDependencyForSBOM(bankApiLibrary)
        apiProjectDependencyForSBOM(captureSdk)
        apiProjectDependencyForSBOM(captureSdkDefaultNetwork)
    } else {
        api(bankApiLibrary)
        api(captureSdk)
        api(captureSdkDefaultNetwork)
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.tools.uiToolingPreview)
    implementation(libs.accompanist.themeAdapter)
    debugImplementation(libs.compose.tools.uiTooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.jUnitParams)

    androidTestImplementation(libs.moshi.core)
    kaptAndroidTest(libs.moshi.codegen)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}


apply<PublishToMavenPlugin>()
apply<DokkaPlugin>()
apply<CodeAnalysisPlugin>()
apply<SBOMPlugin>()

tasks.getByName<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
    this.moduleName.set("Gini Bank SDK for Android")
}

tasks.register<CreatePropertiesTask>("injectTestProperties") {
    val propertiesMap = mutableMapOf<String, String>()

    doFirst {
        propertiesMap.clear()
        propertiesMap.putAll(readLocalPropertiesToMapSilent(project,
            listOf("testClientId", "testClientSecret", "testApiUri", "testUserCenterUri"))
        )
    }

    destinations.put(
        file("src/androidTest/assets/test.properties"),
        propertiesMap
    )
}

afterEvaluate {
    tasks.filter { it.name.endsWith("androidTest", ignoreCase = true) }.forEach {
        it.dependsOn(tasks.getByName("injectTestProperties"))
    }
}
