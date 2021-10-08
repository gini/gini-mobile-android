package net.gini.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.nio.charset.Charset
import java.util.*

/**
 * Created by Alpár Szotyori on 01.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class PropertiesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
    }
}

fun readLocalPropertiesToMap(project: Project, propertyNames: List<String>): Map<String, String> {
    val localProps = readProperties(project, "local.properties")

    checkAllPropertiesAvailable(propertyNames, localProps, project)

    return propertyNames.fold(mutableMapOf()) { map, name ->
        map[name] = localProps.getProperty(name) ?: project.extra[name] as String
        return@fold map
    }
}

fun loadLocalProperties(project: Project, propertyNames: List<String>) {
    val localProps = readProperties(project, "local.properties")

    checkAllPropertiesAvailable(propertyNames, localProps, project)

    localProps.forEach { name, value ->
        if (project.hasProperty(name as String)) {
            project.logger.warn("Local properties will overwrite existing project property $name")
        }
        project.extra[name] = value
    }
}

private fun checkAllPropertiesAvailable(propertyNames: List<String>, properties: Properties, project: Project) {
    propertyNames.forEach { name ->
        if (!properties.containsKey(name) && !project.hasProperty(name)) {
            throw InvalidUserDataException(
                """
                Missing property "$name" from local.properties. You can either:
                  * create a local.properties file in ${project.projectDir} and add the missing property to it, or
                  * pass in the property using "-P$name=<value>".
            """.trimIndent()
            )
        }
    }
}

private fun readProperties(project: Project, propertiesPath: String): Properties {
    val propertiesFile = project.file(propertiesPath)

    if (!propertiesFile.exists()) {
        throw IllegalArgumentException("No properties file set. Configure the LoadPropertiesTask and set a localPropertiesPath.")
    }

    val props = Properties()
    props.load(propertiesFile.inputStream())

    return props
}

abstract class CreatePropertiesTask : DefaultTask() {

    @get:Input
    abstract val destinations: MapProperty<File, Map<String, String>>

    @TaskAction
    fun run() {
        if (destinations.get().isEmpty()) {
            throw IllegalArgumentException("No destination properties files found. Configure the CreatePropertiesTask to add destinations.")
        }

        destinations.get().forEach { (destinationFile, propertiesMap) ->
            if (!destinationFile.exists()) {
                destinationFile.parentFile.mkdir()
                destinationFile.createNewFile()
            }

            val properties = Properties()
            propertiesMap.entries.fold(properties) { props, entry ->
                props[entry.key] = entry.value
                return@fold props
            }

            val comment = """
                    DO NOT COMMIT THIS FILE TO GIT!
    
                    Automatically created by CreatePropertiesTask.
                """.trimIndent()

            properties.store(destinationFile.writer(Charset.forName("UTF-8")), comment)
        }
    }
}