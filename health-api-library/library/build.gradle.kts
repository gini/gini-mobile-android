import net.gini.gradle.DokkaPlugin
import net.gini.gradle.MavenPublishPlugin
import net.gini.gradle.Versions

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
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.preference:preference-ktx:1.1.1")

    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")
    implementation("com.squareup.moshi:moshi:1.12.0")

    // Mocks for testing.
    androidTestImplementation("org.mockito:mockito-core:3.10.0")
    androidTestImplementation("org.mockito:mockito-android:3.10.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.multidex:multidex:2.0.1")
}

apply<MavenPublishPlugin>()
apply<DokkaPlugin>()

// TODO: fix
//apply from: file("repository.gradle")
//
//// TODO: create test.properties file
//def getLocalProperties = {
//    File propertiesFile = file('local.properties')
//    if (propertiesFile.exists()) {
//        Properties properties = new Properties()
//        propertiesFile.withInputStream { instr ->
//            properties.load(instr)
//        }
//        return properties
//    }
//}
//
//def setProperty(key, props, localProps) {
//    if (project.hasProperty(key)) {
//        props[key] = project.property(key)
//    } else {
//        props[key] = localProps?.get(key) ?: ''
//    }
//}
//
//task createTestPropertyFile {
//    doLast {
//        def propertyFile = new File("$projectDir/src/androidTest/assets/test.properties")
//        if (!propertyFile.exists()) propertyFile.createNewFile()
//        def props = new Properties()
//
//        def localProperties = getLocalProperties()
//
//        setProperty('testClientId', props, localProperties)
//        setProperty('testClientSecret', props, localProperties)
//        setProperty('testClientIdAccounting', props, localProperties)
//        setProperty('testClientSecretAccounting', props, localProperties)
//        setProperty('testApiUri', props, localProperties)
//        setProperty('testApiUriAccounting', props, localProperties)
//        setProperty('testUserCenterUri', props, localProperties)
//
//        propertyFile.withWriter("utf-8") {
//            props.store(it, "test properties")
//        }
//    }
//}
//
//tasks.whenTaskAdded { task ->
//    if (task.name.endsWith("Test")) {
//        task.dependsOn.add(createTestPropertyFile)
//    }
//}
//
//
//// TODO: remove this
//apply from: rootProject.file('gradle/javadoc_coverage.gradle')