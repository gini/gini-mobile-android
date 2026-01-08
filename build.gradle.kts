// Top-level build file where you can add configuration options common to all sub-projects/modules.

import net.gini.gradle.ReleaseOrderPlugin
import net.gini.gradle.DependencyUpdatesPlugin

plugins {
    alias(libs.plugins.devtools.ksp) apply false
    id("org.sonarqube") version "5.1.0.4882" apply false
}

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
        classpath(libs.hilt.plugin)
        classpath(libs.cyclonedx.gradle)
        classpath(libs.navigation.safe.args)
        classpath(libs.tomlj)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}
// build.gradle.kts (root)
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
                useVersion("1.26.1")
                because("Avoid NoSuchMethodError in Sonar task due to mismatched commons-compress")
            }
        }
    }
}

apply<DependencyUpdatesPlugin>()
apply<ReleaseOrderPlugin>()