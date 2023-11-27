package net.gini.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.project

/**
 * Created by Alpár Szotyori on 04.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.forEachAndroidProject(runAfterEvaluate: Boolean = false, action: (Project) -> Unit) {
    this.childProjects.forEach { (_, childProject) ->
        if (runAfterEvaluate) {
            childProject.afterEvaluate {
                if (childProject.isAndroidProject) action(childProject)
            }
        } else {
            if (childProject.isAndroidProject) action(childProject)
        }
        childProject.forEachAndroidProject(runAfterEvaluate, action)
    }
}

internal val Project.isAndroidProject: Boolean
    get() = plugins.hasPlugin("com.android.library") || plugins.hasPlugin("com.android.application")

/**
 * Adds a project dependency to the 'api' configuration via it's maven coordinates: group, artifactId and version.
 *
 * This is required for generating a CycloneDX SBOM.
 */
fun DependencyHandler.apiProjectDependencyForSBOM(project: ProjectDependency) {
    projectDependencyForSBOM("api", project)
}

/**
 * Adds a project dependency to the 'implementation' configuration via it's maven coordinates: group, artifactId and version.
 *
 * This is required for generating a CycloneDX SBOM.
 */
fun DependencyHandler.implementationProjectDependencyForSBOM(project: ProjectDependency) {
    projectDependencyForSBOM("implementation", project)
}

private fun DependencyHandler.projectDependencyForSBOM(configurationName: String, project: ProjectDependency) {
    val groupId = project.dependencyProject.properties["groupId"]
    val artifactId = project.dependencyProject.properties["artifactId"]
    val version = project.dependencyProject.version
    add(configurationName, "$groupId:$artifactId:$version")
}
