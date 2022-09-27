package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.Lint
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import net.gini.gradle.extensions.libs
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.kotlin.dsl.findByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Created by Alpár Szotyori on 04.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class CodeAnalysisPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        configureCheckstyle(target)
        configurePmd(target)
        configureDetekt(target)
        configureAndroidLint(target)
        configureKtlint(target)
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
            exclude("**/androidTest/**", "**/test/**")
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
            exclude("**/androidTest/**", "**/test/**")
        }
    }

    private fun configureAndroidLint(target: Project) {
        configureAndroidLint(target, target.extensions.findByType<LibraryExtension>()?.lint)
        configureAndroidLint(target, target.extensions.findByType<ApplicationExtension>()?.lint)
    }

    private fun configureAndroidLint(target: Project, lint: Lint?) {
        lint?.apply {
            abortOnError = false
            lintConfig = target.file("${target.rootDir}/buildSrc/config/lint/lint.xml")
        }
    }

    private fun configureDetekt(target: Project) {
        target.plugins.apply("io.gitlab.arturbosch.detekt")

        target.extensions.getByType<DetektExtension>().apply {
            isIgnoreFailures = true
            source = target.files("src/main/java")
        }
    }

    private fun configureKtlint(target: Project) {
        target.plugins.apply("org.jlleitschuh.gradle.ktlint")

        target.extensions.getByType<KtlintExtension>().apply {
            ignoreFailures.set(true)
            outputToConsole.set(false)
            android.set(true)
            reporters {
                reporter(ReporterType.HTML)
            }
        }
    }

}