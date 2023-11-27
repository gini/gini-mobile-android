package net.gini.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import org.json.JSONArray
import org.json.JSONObject

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
                val artifactId = project.properties["artifactId"] as String
                val projectPURL = "pkg:maven/${project.group}/$artifactId@${project.version}?type=aar"
                val generatedPURL = "pkg:maven/${project.group}/${project.name}@${project.version}?type=jar"

                val bomFile = project.file("build/reports/$outputName.json")
                val bom = bomFile.readText()

                val bomJson = JSONObject(bom)
                val metadataJson = bomJson["metadata"] as JSONObject

                fixComponentName(bomJson, artifactId)

                fixPURLAndRefs(
                    bomJson = bomJson,
                    incorrectPURL = generatedPURL,
                    correctPURL = projectPURL)

                renameManufactureToSupplier(metadataJson)

                addAuthors(metadataJson)

                bomFile.writeText(bomJson.toString(4))
            }
        }
    }

    private fun fixComponentName(bomJson: JSONObject, artifactId: String,) {
        val metadataJson = bomJson["metadata"] as JSONObject
        val componentJson = metadataJson["component"] as JSONObject
        componentJson.put("name", artifactId)
    }

    private fun fixPURLAndRefs(
        bomJson: JSONObject,
        incorrectPURL: String,
        correctPURL: String,
    ) {
        val metadataJson = bomJson["metadata"] as JSONObject
        val componentJson = metadataJson["component"] as JSONObject
        componentJson.put("purl", correctPURL)
        componentJson.put("bom-ref", correctPURL)

        val dependenciesJson = bomJson["dependencies"] as JSONArray
        dependenciesJson.forEach {
            val dependencyJson = it as JSONObject
            if (dependencyJson["ref"].toString() == incorrectPURL) {
                dependencyJson.put("ref", correctPURL)
            }
        }
    }

    private fun renameManufactureToSupplier(metadataJson: JSONObject) {
        val manufactureJson = metadataJson["manufacture"] as JSONObject
        metadataJson.remove("manufacture")
        metadataJson.put("supplier", manufactureJson)
    }

    private fun addAuthors(metadataJson: JSONObject) {
        metadataJson.put(
            "authors", JSONArray(
                listOf(
                    JSONObject(
                        mapOf(
                            "name" to "Gini GmbH"
                        )
                    )
                )
            )
        )
    }

}