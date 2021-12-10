package net.gini.gradle

import com.github.benmanes.gradle.versions.reporter.result.DependencyOutdated
import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.gini.gradle.extensions.forEachAndroidProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.getByName

/**
 * Created by Alpár Szotyori on 11.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

class VersionsPlugin : Plugin<Project> {

    private val outdatedDependenciesMap: MutableMap<String, Pair<DependencyOutdated, MutableSet<Project>>> =
        mutableMapOf()

    override fun apply(target: Project) {
        val clearOutdatedDependenciesMapTask = target.tasks.create("clearOutdatedDependenciesMap") {
            doFirst {
                outdatedDependenciesMap.clear()
            }
        }
        target.tasks.create("dependencyUpdatesForAndroidProjects") {
            doLast {
                logger.lifecycle(
                    """
                    ----------------------------------------
                    Dependency Updates Grouped By Dependency
                    ----------------------------------------
                    
                    The following dependencies have later milestone versions:
                """.trimIndent()
                )

                outdatedDependenciesMap.forEach { (_, v) ->
                    val (outdatedDependency, projects) = v
                    logger.lifecycle(" - ${outdatedDependency.group}:${outdatedDependency.name} [${outdatedDependency.version} -> ${outdatedDependency.available.milestone}]")
                    logger.lifecycle("   Projects:")
                    projects.forEach { project ->
                        logger.lifecycle("    - ${project.path}")
                    }
                }
            }
            dependsOn(clearOutdatedDependenciesMapTask)
        }

        applyPluginToAllAndroidProjects(
            target = target,
            parentProject = target,
            pluginId = "com.github.ben-manes.versions"
        )
    }

    private fun applyPluginToAllAndroidProjects(target: Project, parentProject: Project, pluginId: String) {
        parentProject.forEachAndroidProject(runAfterEvaluate = true) { androidProject ->
            androidProject.plugins.apply(pluginId)

            val dependencyUpdateTask =
                androidProject.tasks.getByName<DependencyUpdatesTask>("dependencyUpdates").apply {
                    rejectVersionIf { isNotSemver(candidate.version) }
                    this.outputFormatter = closureOf<Result> {
                        this.outdated.dependencies.forEach { outdatedDependency ->
                            outdatedDependenciesMap[outdatedDependency.coordinate]?.second?.add(androidProject) ?: run {
                                outdatedDependenciesMap[outdatedDependency.coordinate] =
                                    outdatedDependency to mutableSetOf(androidProject)
                            }
                        }
                    }

                }

            target.tasks.getByName("dependencyUpdatesForAndroidProjects")
                .dependsOn(dependencyUpdateTask)
        }
    }

    private fun isNotSemver(version: String): Boolean = !(semverRegex matches version)

    companion object {
        private val semverRegex = """^[0-9]+\.[0-9]+\.[0-9]+$""".toRegex()
    }
}

private val DependencyOutdated.coordinate: String
    get() = "$group:$name:$version"