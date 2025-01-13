import net.gini.gradle.CreatePropertiesTask
import net.gini.gradle.PropertiesPlugin
import net.gini.gradle.readLocalPropertiesToMap

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "net.gini.android.health.sdk.exampleapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // after upgrading to AGP 8, we need this to have the defaultConfig block
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "net.gini.android.health.sdk.exampleapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk =libs.versions.android.targetSdk.get().toInt()

        versionName = version as String
        versionCode = (properties["versionCode"] as? String)?.toInt() ?: 0

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")
        }
    }
    flavorDimensions += "environment"
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
        // Fix for "Inheritance from an interface with '@JvmDefault' members is only allowed with -Xjvm-default option"
        // https://issuetracker.google.com/issues/217593040#comment6
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// after upgrading to AGP 8, we need this, otherwise, gradle will complain to use the same jdk version as your machine (17 which is bundled with Android Studio)
// https://youtrack.jetbrains.com/issue/KT-55947/Unable-to-set-kapt-jvm-target-version
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

dependencies {

    implementation(project(":health-sdk:sdk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.koin.androidx.scope)
    implementation(libs.koin.androidx.viewmodel)
    implementation(libs.koin.androidx.fragment)
    implementation(libs.insetter)
    implementation(libs.datastore.preferences)
    implementation(libs.moshi.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    kapt(libs.moshi.codegen)
    implementation(libs.logback.android)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

apply<PropertiesPlugin>()

tasks.register<CreatePropertiesTask>("injectClientCredentials") {
    val propertiesMap = mutableMapOf<String, String>()

    doFirst {
        propertiesMap.clear()
        propertiesMap.putAll(readLocalPropertiesToMap(project, listOf("clientId", "clientSecret")))
    }

    destinations.put(
        file("src/main/resources/client.properties"),
        propertiesMap
    )
}

afterEvaluate {
    tasks.filter { it.name.startsWith("assemble", ignoreCase = true) }.forEach {
        it.dependsOn(tasks.getByName("injectClientCredentials"))
    }
}
