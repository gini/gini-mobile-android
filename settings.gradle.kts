enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For PhotoView
        maven { url = java.net.URI.create("https://jitpack.io") }
    }
}

rootProject.name = "Gini Mobile Android"

include("health-api-library:library")
include("health-sdk:sdk")
include("health-sdk:example-app")
