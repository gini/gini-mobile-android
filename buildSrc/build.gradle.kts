plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    // For org.jlleitschuh.gradle:ktlint-gradle
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.detekt.gradle)
    implementation(libs.ktlint.gradle)
    implementation(libs.benManesVersions.gradle)
    implementation(libs.java.poet)
    implementation(libs.cyclonedx.gradle)
    implementation(libs.cyclonedx.core.java)
}