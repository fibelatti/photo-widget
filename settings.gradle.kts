@file:Suppress("UnstableApiUsage")

rootProject.name = "PhotoWidget"
rootProject.buildFileName = "build.gradle.kts"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":ui")

pluginManagement {
    repositories {
        google {
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
        maven(url = "https://jitpack.io") {
            mavenContent {
                includeGroup("com.github.yalantis")
            }
        }

        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }

        mavenCentral()
    }
}
