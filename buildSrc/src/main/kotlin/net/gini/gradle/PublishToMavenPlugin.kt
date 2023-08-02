package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI
import java.util.*

/**
 * Created by Alpár Szotyori on 30.09.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class PublishToMavenPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        //configureMavenPublish(target)

       /* target.tasks.create("printVersion") {
            doLast {
                val version: String by target
                println(version)
            }
        }*/
    }

    /*private fun configureMavenPublish(target: Project) {
        target.plugins.apply("maven-publish")
        target.plugins.apply("signing")

        *//*val sourcesJar = target.tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            val library = target.extensions.getByType<LibraryExtension>()
            from(library.sourceSets["main"].java.srcDirs)
        }

        val javadocJar = target.tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            val dokkaJavadoc = target.tasks.getByName<DokkaTask>("dokkaJavadoc")
            dependsOn(dokkaJavadoc)
            from(dokkaJavadoc.outputDirectory)
        }*//*

        target.afterEvaluate {
            //val sourcesArtifact = artifacts.add("archives", sourcesJar)
            //val javadocArtifact = artifacts.add("archives", javadocJar)

            extensions.getByType<PublishingExtension>().apply {
                publications {
                    // Creates a Maven publication called "release"
                    create<MavenPublication>("release") {
                        // Applies the component for the release build variant
                        from(components["release"])

                        // Adds additional artifacts
                        //artifact(sourcesArtifact)
                        //artifact(javadocArtifact)

                        // Customizes attributes of the publication
                        val groupId: String by target
                        val artifactId: String by target
                        val version: String by target
                        this.groupId = groupId
                        this.artifactId = artifactId
                        this.version = version

                        pom {
                            name.set(getPropertyOrEmpty("pomName"))
                            description.set(getPropertyOrEmpty("pomDescription"))
                            url.set(getPropertyOrEmpty("pomUrl"))
                            licenses {
                                license {
                                    name.set(getPropertyOrEmpty("pomLicenseName"))
                                    url.set(getPropertyOrEmpty("pomLicenseUrl"))
                                }
                            }
                            developers {
                                developer {
                                    id.set(getPropertyOrEmpty("pomDeveloperId"))
                                    name.set(getPropertyOrEmpty("pomDeveloperName"))
                                    email.set(getPropertyOrEmpty("pomDeveloperEmail"))
                                }
                            }
                            scm {
                                connection.set(getPropertyOrEmpty("pomScmConnection"))
                                developerConnection.set(getPropertyOrEmpty("pomScmDeveloperConnection"))
                                url.set(getPropertyOrEmpty("pomScmUrl"))
                            }
                        }
                    }
                }

                val signingKey: String? by target
                val signingPassword: String? by target

                if (signingKey != null && signingPassword != null) {
                    extensions.getByType<SigningExtension>().apply {
                        useInMemoryPgpKeys(signingKey, signingPassword)
                        sign(publications["release"])
                    }
                } else {
                    logger.warn(
                        """
                            WARNING:
                            Maven release will not be signed.
                            Pass in -PsigningKey=<key> and -PsigningPassword=<password> to sign the release.
                            See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys.
                        """.trimIndent()
                    )
                }

                repositories {

                    fun addMavenRepository(
                        repoUrlPropertyName: String,
                        repoName: String,
                        requiresCredentials: Boolean = true,
                        required: Boolean = false
                    ) {
                        properties[repoUrlPropertyName]?.let { repoUrl ->
                            maven {
                                name = repoName
                                url = URI.create(repoUrl as String)
                                if (requiresCredentials) {
                                    credentials {
                                        username = properties["repoUser"] as? String ?: throw InvalidUserDataException(
                                            """
                                                    Missing maven repo username for "$repoUrl". 
                                                    You need to pass it in using "-PrepoUser=<user>".
                                                """.trimIndent()
                                        )

                                        password =
                                            properties["repoPassword"] as? String ?: throw InvalidUserDataException(
                                                """
                                                    Missing maven repo password for "$repoUrl". 
                                                    You need to pass it in using "-PrepoPassword=<password>".
                                                """.trimIndent()
                                            )
                                    }
                                }
                            }
                        } ?: if (required) logger.warn(
                            """
                                    WARNING:
                                    Missing property "$repoUrlPropertyName".
                                    You need to pass it in using "-P$repoUrlPropertyName=<url>" to be able to 
                                    use the "publishReleasePublicationTo${repoName.capitalize(Locale.getDefault())}Repository" task.
                                    
                                """.trimIndent()
                        ) else {

                        }
                    }

                    addMavenRepository(
                        repoUrlPropertyName = "mavenReleasesRepoUrl",
                        repoName = "releases",
                        required = true
                    )
                    addMavenRepository(repoUrlPropertyName = "mavenSnapshotsRepoUrl", repoName = "snapshots")
                }
            }
        }
    }*/

}

private fun Project.getPropertyOrEmpty(property: String): String =
    properties[property] as? String ?: run {
            logger.warn(
                """
                    WARNING:
                    Missing property for maven publishing: $property 
                    You need to pass it in using "-P$property=<value>" or add it to gradle.properties.
                """.trimIndent()
            )
            ""
    }
