package net.gini.gradle

import net.gini.gradle.extensions.mapProperty
import net.gini.gradle.extensions.property
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.WriteProperties
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import java.time.ZonedDateTime

/**
 * Created by Alpár Szotyori on 01.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class TestPropertiesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.create<TestPropertiesPluginExtension>("testProperties", target)

        target.tasks.register<Copy>("copyTestProperties") {
            val extension = target.extensions.getByName<TestPropertiesPluginExtension>("testProperties")

            val sourceFile = target.file(extension.sourceFilePath.get())
            val targetFile = target.file(extension.targetFilePath.get())

            from(sourceFile)
            rename { targetFile.name }
            destinationDir = targetFile.parentFile
        }

        target.tasks.register<WriteProperties>("injectTestProperties") {
            val extension = target.extensions.getByName<TestPropertiesPluginExtension>("testProperties")

            outputFile = target.file(extension.targetFilePath.get())
            encoding = "UTF-8"
            comment = """
                    DO NOT COMMIT THIS FILE TO GIT!
    
                    Automatically injected by injectTestProperties at ${ZonedDateTime.now()}.
                """.trimIndent()

            val properties = extension.properties.get()

            if (properties.isEmpty()) {
                throw IllegalArgumentException("No test properties found. Configure the TestPropertiesPluginExtension to add properties.")
            }

            properties(properties as Map<String, Any>)
        }
    }
}

open class TestPropertiesPluginExtension internal constructor(project: Project) {

    val properties: MapProperty<String, String> = project.objects.mapProperty()

    val sourceFilePath: Property<String> = project.objects.property {
        set("local.properties")
    }
    val targetFilePath: Property<String> = project.objects.property {
        set("src/androidTest/assets/test.properties")
    }
}