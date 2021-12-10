// Top-level build file where you can add configuration options common to all sub-projects/modules.

import net.gini.gradle.ReleaseOrderPlugin
import net.gini.gradle.VersionsPlugin

buildscript {
    repositories {
        google()
        mavenCentral()
        // For org.jlleitschuh.gradle:ktlint-gradle
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.kotlin.gradle)
        classpath(libs.dokka.gradle)
        classpath(libs.dokka.kotlinAsJava)
        classpath(libs.detekt.gradle)
        classpath(libs.ktlint.gradle)
        classpath(libs.jacocoAndroid)
        classpath(libs.benManesVersions.gradle)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

apply<VersionsPlugin>()
apply<ReleaseOrderPlugin>()