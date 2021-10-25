enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For PhotoView
        maven("https://jitpack.io")
    }
}

rootProject.name = "Gini Mobile Android"

include("core-api-library:library")

include("health-api-library:library")

include("health-sdk:sdk")
include("health-sdk:example-app")
