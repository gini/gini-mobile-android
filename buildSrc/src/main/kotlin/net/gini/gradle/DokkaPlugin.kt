package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import net.gini.gradle.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
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
            add("dokkaHtmlPlugin", target.libs.findDependency("dokka.kotlinAsJava").get())
        }

        target.tasks.create<DokkaCollectorTask>("dokkaHtmlSiblingCollector") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka"))

            addChildTask("${target.path}:dokkaHtml")

            (target.configurations.getByName("api").dependencies +
                    target.configurations.getByName("implementation").dependencies)
                .filterIsInstance<ProjectDependency>()
                .forEach { addChildTask("${it.dependencyProject.path}:dokkaHtml") }
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

            (target.configurations.getByName("api").dependencies +
                    target.configurations.getByName("implementation").dependencies)
                .filterIsInstance<ProjectDependency>()
                .forEach { addChildTask("${it.dependencyProject.path}:dokkaJavadoc") }
        }

        target.tasks.getByName<DokkaTask>("dokkaJavadoc") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka-javadoc"))
        }

    }

}