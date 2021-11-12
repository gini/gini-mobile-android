package net.gini.gradle

import net.gini.gradle.extensions.forEachAndroidProject
import net.gini.gradle.extensions.isAndroidProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

/**
 * Created by Alpár Szotyori on 12.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class ProjectDependenciesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.create("listReleaseOrder") {
            doLast {
                generateReleaseOrder(project) { outputLine ->
                    project.logger.lifecycle(outputLine)
                }
            }
        }

        val updateReleaseOrderFileTask = target.tasks.create("updateReleaseOrderFile") {
            doLast {
                val releaseOrder = File("${project.rootDir}/RELEASE-ORDER.md")
                releaseOrder.bufferedWriter().use { writer ->
                    writer.write("DO NOT EDIT MANUALLY!\nAutomatically created by the updateReleaseOrderFile task.\n\n")
                    generateReleaseOrder(project) { outputLine ->
                        writer.write("$outputLine\n")
                    }
                }
            }
        }

        target.forEachAndroidProject(runAfterEvaluate = true) { androidProject ->
            androidProject.tasks.findByName("preBuild")?.dependsOn(updateReleaseOrderFileTask)
        }
    }

    private fun generateReleaseOrder(project: Project, writeLine: (String) -> Unit) {
        val releaseOrders = mutableMapOf<String, MutableList<String>>()

        project.forEachAndroidProject { androidProject ->
            if (androidProject.path.contains("example")) {
                return@forEachAndroidProject
            }

            if (!releaseOrders.containsKey(androidProject.path)) {
                releaseOrders["${androidProject.path} ${androidProject.version}"] = mutableListOf()
            }

            releaseOrders["${androidProject.path} ${androidProject.version}"]?.addAll(
                getAllAndroidProjectDependencies(androidProject)
                    .map { "${it.dependencyProject.path} ${it.dependencyProject.version}" }
            )
        }

        releaseOrders.toList()
            .sortedBy { (_, value) -> value.size }
            .forEach { (projectPath, dependencyPaths) ->
                writeLine("Release order for $projectPath:")

                var counter = 1
                dependencyPaths.forEach { dependencyPath ->
                    writeLine(" ${counter}. $dependencyPath")
                    counter++
                }

                writeLine(" ${counter}. $projectPath\n")
            }
    }

    private fun getAllAndroidProjectDependencies(project: Project): List<ProjectDependency> {
        val projectDependencies = listOf(
            project.configurations.getByName("api"),
            project.configurations.getByName("implementation")
        )
            .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
            .filter { it.dependencyProject.isAndroidProject }
            .distinct()
        return projectDependencies.flatMap { getAllAndroidProjectDependencies(it.dependencyProject) + listOf(it) }
            .distinct()
    }
}