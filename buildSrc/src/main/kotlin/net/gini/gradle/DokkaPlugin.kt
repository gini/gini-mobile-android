package net.gini.gradle

import com.android.build.gradle.LibraryExtension
import net.gini.gradle.extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.dokka.gradle.DokkaTask

class DokkaPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.dokka")

        target.dependencies {
            add("dokkaHtmlPlugin", target.libs.findDependency("dokka-kotlinAsJavaPlugin").get())
        }

        target.tasks.getByName<DokkaTask>("dokkaHtml") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka"))
            this.dokkaSourceSets.named("main").configure {
                noAndroidSdkLink.set(false)

                val library = target.extensions.getByType<LibraryExtension>()
                val packageDocumentationPaths = library.sourceSets["main"].java.srcDirs.first()
                    .walkTopDown()
                    .filter { it.name == "package.md"}
                    .map { it.absolutePath }
                    .asIterable()

                includes.from(target.files("module.md", packageDocumentationPaths))
            }
        }

        target.tasks.getByName<DokkaTask>("dokkaJavadoc") {
            outputDirectory.set(target.file("${target.buildDir}/docs/dokka-javadoc"))
        }
    }

}