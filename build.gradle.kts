// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Found this "magic" code at https://blog.stylingandroid.com/gradle-version-catalogs/
        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs") as org.gradle.accessors.dm.LibrariesForLibs

        classpath(libs.android.gradleplugin)
        classpath(libs.kotlin.gradleplugin)
        classpath(libs.dokka.gradleplugin)
        classpath(libs.dokka.kotlinAsJavaPlugin)

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
