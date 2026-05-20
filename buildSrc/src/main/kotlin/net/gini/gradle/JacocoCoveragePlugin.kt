package net.gini.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarExtension

class JacocoCoveragePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.afterEvaluate { configureJacoco(this) }
    }

    private fun configureJacoco(project: Project) {
        val buildDir = project.layout.buildDirectory
        val xmlReportFile = buildDir.file("reports/jacoco/jacocoTestReport.xml")

        // Robolectric's SandboxClassLoader defines classes without a CodeSource location.
        // Without this flag, JaCoCo's agent skips those classes entirely, yielding 0% coverage.
        project.tasks.withType(Test::class.java).configureEach {
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }

        project.tasks.register("jacocoTestReport", JacocoReport::class.java) {
            group = "Reporting"
            description = "Generate JaCoCo XML coverage report for SonarCloud."
            dependsOn("testDebugUnitTest")

            reports {
                xml.required.set(true)
                html.required.set(true)
                xml.outputLocation.set(xmlReportFile.get().asFile)
            }

            // The Gradle JaCoCo plugin attaches the agent to Test tasks, writing exec data here.
            // isTestCoverageEnabled must be false; AGP's offline instrumentation conflicts with
            // Robolectric's SandboxClassLoader and produces 0% coverage.
            executionData.setFrom(
                buildDir.file("jacoco/testDebugUnitTest.exec")
            )

            val excludes = listOf(
                "**/R.class", "**/R\$*.class",
                "**/BuildConfig.*", "**/Manifest*.*",
                "**/*Test*.*", "android/**/*.*",
                "**/*_MembersInjector*.*", "**/*Dagger*.*", "**/*_Factory*.*",
                "**/databinding/**", "**/*DataBinding*.*",
                "**/*ComposableSingletons*.*"
            )

            // Java class files — AGP 8 path
            classDirectories.setFrom(
                project.fileTree(
                    buildDir.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
                ) { exclude(excludes) },
                // Kotlin class files — AGP 8 path
                project.fileTree(
                    buildDir.dir("tmp/kotlin-classes/debug")
                ) { exclude(excludes) }
            )

            sourceDirectories.setFrom(
                project.files("src/main/java", "src/main/kotlin")
            )
        }

        project.extensions.findByType(SonarExtension::class.java)?.properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                xmlReportFile.get().asFile.absolutePath
            )
            val sourceDirs = listOf("src/main/java", "src/main/kotlin")
                .filter { project.file(it).exists() }
                .joinToString(",")
            if (sourceDirs.isNotEmpty()) {
                property("sonar.sources", sourceDirs)
            }
        }
    }
}
