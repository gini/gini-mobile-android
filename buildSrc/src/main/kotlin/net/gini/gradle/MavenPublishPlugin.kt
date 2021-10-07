package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

/**
 * Created by Alpár Szotyori on 30.09.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
class MavenPublishPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply("maven-publish")

        val sourcesJar = target.tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            val library = target.extensions.getByType<LibraryExtension>()
            from(library.sourceSets["main"].java.srcDirs)
        }

        val javadocJar = target.tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            val dokkaJavadoc = target.tasks.getByName<DokkaTask>("dokkaJavadoc")
            dependsOn(dokkaJavadoc)
            from(dokkaJavadoc.outputDirectory)
        }

        target.afterEvaluate {
            val sourcesArtifact = artifacts.add("archives", sourcesJar)
            val javadocArtifact = artifacts.add("archives", javadocJar)

            extensions.getByType<PublishingExtension>().apply {
                publications {
                    // Creates a Maven publication called "release"
                    create<MavenPublication>("release") {
                        // Applies the component for the release build variant
                        from(components["release"])

                        // Adds additional artifacts
                        artifact(sourcesArtifact)
                        artifact(javadocArtifact)

                        // Customizes attributes of the publication
                        val groupId: String by target
                        val artifactId: String by target
                        val version: String by target
                        this.groupId = groupId
                        this.artifactId = artifactId
                        this.version = version
                    }
                }

                repositories {

                    fun addMavenRepository(repoUrlPropertyName: String, repoName: String, requiresCredentials: Boolean = true) {
                        project.properties[repoUrlPropertyName]?.let { repoUrl ->
                            maven {
                                name = repoName
                                url = URI.create(repoUrl as String)
                                if (requiresCredentials) {
                                    credentials {
                                        username = project.properties["repoUser"] as? String ?: "invalidUsername"
                                        password = project.properties["repoPassword"] as? String ?: "invalidPassword"
                                    }
                                }
                            }
                        }
                    }

                    addMavenRepository(repoUrlPropertyName = "mavenOpenRepoUrl", repoName = "open")
                    addMavenRepository(repoUrlPropertyName = "mavenSnapshotsRepoUrl", repoName = "snapshots")
                    addMavenRepository(repoUrlPropertyName = "mavenLocalRepoUrl", repoName = "local",
                        requiresCredentials = false)
                }
            }
        }
    }

}

