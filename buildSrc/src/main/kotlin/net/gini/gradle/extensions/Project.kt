package net.gini.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

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
