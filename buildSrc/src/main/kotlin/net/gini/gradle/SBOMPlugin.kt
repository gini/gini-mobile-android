package net.gini.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName

/**
 * Uses the CycloneDX Gradle plugin to generate an SBOM (Software Bill of Materials) for the project.
 *
 * Applies the necessary configuration and edits the generated SBOM to use the artifactId instead of the project name and "aar" type instead of "jar".
 */
class SBOMPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        if (target.properties["createSBOM"] != "true") {
            return
        }

        target.plugins.apply("org.cyclonedx.bom")

        target.group = target.properties["groupId"] as String

        target.tasks.getByName<org.cyclonedx.gradle.CycloneDxTask>("cyclonedxBom") {
            setSchemaVersion("1.4")

            setIncludeConfigs(listOf(
                "releaseRuntimeClasspath",
                "releaseCompileClasspath"
            ))

            val outputName = "${project.properties["artifactId"]}-sbom"
            setOutputName(outputName)

            setOrganizationalEntity {
                it.name = "Gini GmbH"
                it.urls = listOf("https://gini.net")
            }

            doLast {
                val bomFile = project.file("build/reports/$outputName.json")
                val bom = bomFile.readText()
                val editedBom = bom.replace(
                    """
                        "name" : "${project.name}"
                    """.trimIndent(),
                    """
                        "name" : "${project.properties["artifactId"]}"
                    """.trimIndent()
                ).replace(
                    """
                        "purl" : "pkg:maven/${project.group}/${project.name}@${project.version}?type=jar"
                    """.trimIndent(),
                    """
                        "purl" : "pkg:maven/${project.group}/${project.properties["artifactId"]}@${project.version}?type=aar"
                    """.trimIndent()
                ).replace(
                    """
                        "bom-ref" : "pkg:maven/${project.group}/${project.name}@${project.version}?type=jar"
                    """.trimIndent(),
                    """
                        "bom-ref" : "pkg:maven/${project.group}/${project.properties["artifactId"]}@${project.version}?type=aar"
                    """.trimIndent()
                ).replace(
                    """
                        "ref" : "pkg:maven/${project.group}/${project.name}@${project.version}?type=jar"
                    """.trimIndent(),
                    """
                        "ref" : "pkg:maven/${project.group}/${project.properties["artifactId"]}@${project.version}?type=aar"
                    """.trimIndent()
                )
              bomFile.writeText(editedBom)
            }
        }
    }

}