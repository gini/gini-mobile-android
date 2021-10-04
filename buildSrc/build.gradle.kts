plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.android.gradleplugin)
    implementation(libs.kotlin.gradleplugin)
    implementation(libs.dokka.gradleplugin)
}