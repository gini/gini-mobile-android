package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import net.gini.gradle.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaCollectorTask
import org.jetbrains.dokka.gradle.DokkaTask

/**
 * Created by Alpár Szotyori on 30.09.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class DokkaPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.dokka")

        target.dependencies {
            add("dokkaHtmlPlugin", target.libs.findLibrary("dokka.kotlinAsJava").get())
        }

        target.tasks.create<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka"))

            addChildTask("${target.path}:dokkaHtml")

            addSiblingDokkaTasksRecursive(target.configurations.asMap["api"]?.dependencies, "dokkaHtml")
        }

        target.tasks.getByName<DokkaTask>("dokkaHtml") {
            this.dokkaSourceSets.named("main").configure {
                noAndroidSdkLink.set(false)

                val library = target.extensions.getByType<LibraryExtension>()
                val packageDocumentationPaths = library.sourceSets["main"].java.srcDirs.first()
                    .walkTopDown()
                    .filter { it.name == "package.md"}
                    .map { it.absolutePath }
                    .asIterable()

                includes.from(target.files("module.md", packageDocumentationPaths))
            }
        }

        target.tasks.create<DokkaCollectorTask>("dokkaJavadocSiblingCollector") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka"))

            addChildTask("${target.path}:dokkaJavadoc")

            addSiblingDokkaTasksRecursive(target.configurations.asMap["api"]?.dependencies, "dokkaJavadoc")
        }

        target.tasks.getByName<DokkaTask>("dokkaJavadoc") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka-javadoc"))
        }

    }
}

private fun DokkaCollectorTask.addSiblingDokkaTasksRecursive(dependencies: Set<Dependency>?, dokkaTaskName: String) {
    dependencies
        ?.filterIsInstance<ProjectDependency>()
        ?.forEach {
            addChildTask("${it.dependencyProject.path}:$dokkaTaskName")
            addSiblingDokkaTasksRecursive(
                it.dependencyProject.configurations.asMap["api"]?.dependencies,
                dokkaTaskName)
        }
}