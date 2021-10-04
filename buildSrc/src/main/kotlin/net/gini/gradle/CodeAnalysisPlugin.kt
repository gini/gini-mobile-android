package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import net.gini.gradle.extensions.libs
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension

class CodeAnalysisPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        configureCheckstyle(target)
        configurePmd(target)
        configureAndroidLint(target)
        // TODO: add ktlint
        // TODO: add detekt
    }

    private fun configureCheckstyle(target: Project) {
        target.plugins.apply("checkstyle")

        target.extensions.getByType<CheckstyleExtension>().apply {
            toolVersion = target.libs.findVersion("checkstyle").get().requiredVersion
        }

        target.tasks.register<Checkstyle>("checkstyle") {
            isIgnoreFailures = true
            isShowViolations = false
            configFile = target.file("${target.rootDir}/buildSrc/config/checkstyle/checkstyle.xml")
            configProperties?.set(
                "checkstyleSuppressionsPath",
                target.file("${target.rootDir}/buildSrc/config/checkstyle/suppressions.xml").absolutePath
            )
            source = target.fileTree("src")
            include("**/*.java")
            exclude("**/gen/**", "**/androidTest/**", "**/test/**", "**/testShared/**", "**/com/ortiz/**")
            classpath = target.files()
        }
    }

    private fun configurePmd(target: Project) {
        target.plugins.apply("pmd")

        target.extensions.getByType<PmdExtension>().apply {
            toolVersion = target.libs.findVersion("pmd").get().requiredVersion
        }

        target.tasks.register<Pmd>("pmd") {
            ignoreFailures = true
            ruleSetFiles = target.files("${target.rootDir}/buildSrc/config/pmd/pmd-ruleset.xml")
            ruleSets = listOf()

            source = target.fileTree("src")
            include("**/*.java")
            exclude("**/gen/**", "**/androidTest/**", "**/test/**", "**/testShared/**", "**/com/ortiz/**")
        }
    }

    private fun configureAndroidLint(target: Project) {
        target.extensions.getByType<LibraryExtension>().apply {
            lint {
                isAbortOnError = false
                lintConfig = target.file("${target.rootDir}/buildSrc/config/lint/lint.xml")
            }
        }
    }

}