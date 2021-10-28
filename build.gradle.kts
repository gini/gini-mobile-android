// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        // For org.jlleitschuh.gradle:ktlint-gradle
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        // Found this "magic" code at https://blog.stylingandroid.com/gradle-version-catalogs/
        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs") as org.gradle.accessors.dm.LibrariesForLibs

        classpath(libs.android.gradle)
        classpath(libs.kotlin.gradle)
        classpath(libs.dokka.gradle)
        classpath(libs.dokka.kotlinAsJava)
        classpath(libs.detekt.gradle)
        classpath(libs.ktlint.gradle)
        classpath(libs.jacocoAndroid)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}
