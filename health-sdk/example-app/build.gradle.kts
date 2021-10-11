import net.gini.gradle.CreatePropertiesTask
import net.gini.gradle.PropertiesPlugin
import net.gini.gradle.readLocalPropertiesToMap

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.gini.android.health.sdk.exampleapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk =libs.versions.android.targetSdk.get().toInt()
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.koin.androidx.scope)
    implementation(libs.koin.androidx.viewmodel)
    implementation(libs.koin.androidx.fragment)
    implementation(libs.insetter)

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
