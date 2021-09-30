plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Keep versions in sync with Versions.kt
    // We'll switch to version catalogs once it's ready: https://docs.gradle.org/current/userguide/platforms.html
    val agpVersion: String by project
    implementation("com.android.tools.build:gradle:$agpVersion")
    val kotlinVersion: String by project
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    val dokkaVersion: String by project
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}