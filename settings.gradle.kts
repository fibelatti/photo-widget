@file:Suppress("UnstableApiUsage")

rootProject.name = "PhotoWidget"
rootProject.buildFileName = "build.gradle.kts"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":ui")

pluginManagement {
    repositories {
        mavenCentral {
            content {
                excludeGroupByRegex("com\\.android.*")
            }
        }
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven(url = "https://jitpack.io") }
            filter { includeGroup("com.github.yalantis") }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("androidx.*")
                excludeGroupByRegex("com\\.android.*")
            }
        }
        google()
    }
}
