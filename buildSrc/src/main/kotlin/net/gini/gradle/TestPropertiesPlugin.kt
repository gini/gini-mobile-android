package net.gini.gradle

import net.gini.gradle.TestPropertiesPluginExtension.Defaults
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import java.io.File

interface TestPropertiesPluginExtension {
    val source: Property<File>
    val target: Property<File>

    object Defaults {
        const val sourceFile = "local.properties"
        const val targetFile = "src/androidTest/assets/test.properties"
    }
}

class TestPropertiesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.create<TestPropertiesPluginExtension>("testProperties")

        target.tasks.register<Copy>("injectTestProperties") {
            val extension = target.extensions.getByName<TestPropertiesPluginExtension>("testProperties")

            val sourceProperties = extension.source.orNull ?: target.file(Defaults.sourceFile)
            val targetProperties = extension.target.orNull ?: target.file(Defaults.targetFile)

            from(sourceProperties)
            rename { targetProperties.name }
            destinationDir = targetProperties.parentFile
        }
    }
}