rootProject.name = "Photo Widget"
rootProject.buildFileName = "build.gradle.kts"

include(":app")
include(":ui")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral {
            content {
                excludeGroupByRegex("com\\.android.*")
            }
        }
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://jitpack.io") {
            content {
                includeGroup("com.github.yalantis")
            }
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
