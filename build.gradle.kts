// Top-level build file where you can add configuration options common to all sub-projects/modules.

//val agpVersion = "7.0.2".also { extra.set("agpVersion", it) }
//val kotlinVersion = "1.5.0".also { extra.set("kotlinVersion", it) }
//val dokkaVersion = "1.5.0".also { extra.set("dokkaVersion", it) }

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Keep versions in sync with Versions.kt
        // We'll switch to version catalogs once it's ready: https://docs.gradle.org/current/userguide/platforms.html
        val agpVersion: String by project
        classpath("com.android.tools.build:gradle:$agpVersion")
        val kotlinVersion: String by project
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        val dokkaVersion: String by project
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
        classpath("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // For PhotoView
        maven { url = java.net.URI.create("https://jitpack.io") }
    }
}
