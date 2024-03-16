@file:Suppress("UnstableApiUsage")

rootProject.name = "Photo Widget"
rootProject.buildFileName = "build.gradle.kts"

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
