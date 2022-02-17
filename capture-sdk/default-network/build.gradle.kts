import net.gini.gradle.*

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

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
    
    kotlinOptions {
        jvmTarget = "1.8"
        // required to prevent .kotlin_module file name collisions
        // https://stackoverflow.com/a/56329676/276129
        moduleName = "${properties["groupId"]}.${properties["artifactId"]}"
    }
}

dependencies {
    api(libs.slf4j.api)

    api(project(":bank-api-library:library"))

    implementation(project(":capture-sdk:sdk"))

    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.core)

    androidTestImplementation(libs.moshi.core)
    kaptAndroidTest(libs.moshi.codegen)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
}

apply<PublishToMavenPlugin>()
apply<CodeAnalysisPlugin>()
apply<DokkaPlugin>()

tasks.register<CreatePropertiesTask>("injectTestProperties") {
    val propertiesMap = mutableMapOf<String, String>()

    doFirst {
        propertiesMap.clear()
        propertiesMap.putAll(readLocalPropertiesToMap(project,
            listOf("testClientId", "testClientSecret", "testApiUri", "testUserCenterUri")))
    }

    destinations.put(
        file("src/androidTest/assets/test.properties"),
        propertiesMap
    )
}

afterEvaluate {
    tasks.filter { it.name.endsWith("test", ignoreCase = true) }.forEach {
        it.dependsOn(tasks.getByName("injectTestProperties"))
    }
}