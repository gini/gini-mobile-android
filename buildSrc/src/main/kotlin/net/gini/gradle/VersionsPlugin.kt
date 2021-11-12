package net.gini.gradle

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import net.gini.gradle.extensions.forEachAndroidProject

/**
 * Created by Alpár Szotyori on 11.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

class VersionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.create("dependencyUpdatesForAndroidProjects")

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
                    outputDir = "${androidProject.buildDir}/dependencyUpdates"
                    rejectVersionIf { isNotSemver(candidate.version) }
                }

            target.tasks.getByName("dependencyUpdatesForAndroidProjects")
                .dependsOn(dependencyUpdateTask)
        }
    }

    private fun isNotSemver(version: String): Boolean = !( semverRegex matches version)

    companion object {
        private val semverRegex = """^[0-9]+\.[0-9]+\.[0-9]+$""".toRegex()
    }
}
