enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For PhotoView
        maven("https://jitpack.io")
        // For com.hiya:jacoco-android
        maven("https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "Gini Mobile Android"

include("core-api-library:library")
include(":core-api-library:shared-tests")

include("health-api-library:library")

include("bank-api-library:library")

include("health-sdk:sdk")
include("health-sdk:example-app")

include("capture-sdk:sdk")
include("capture-sdk:default-network")
//include("capture-sdk:example-app-shared-code")
include("capture-sdk:screen-api-example-app")

include("bank-sdk:sdk")
include("bank-sdk:screen-api-example-app")
