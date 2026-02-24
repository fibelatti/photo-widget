@file:Suppress("UnstableApiUsage")

rootProject.name = "PhotoWidget"
rootProject.buildFileName = "build.gradle.kts"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":ui")

pluginManagement {
    repositories {
        maven(url = "https://maven.google.com/") {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://maven.google.com/") {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }

        mavenCentral()
    }
}
