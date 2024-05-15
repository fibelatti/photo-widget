import com.android.build.api.dsl.CommonExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.cache.fix) apply false
    alias(libs.plugins.about.libraries) apply false
}

buildscript {
    extra["compileSdkVersion"] = 34
    extra["targetSdkVersion"] = 34
    extra["minSdkVersion"] = 26
}

allprojects {
    afterEvaluate {
        apply(plugin = libs.plugins.spotless.get().pluginId)

        val configureSpotless: SpotlessExtension.() -> Unit = {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")
            }
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**/*.kts")

                ktlint()
            }
            format("misc") {
                target("*.gradle", "*.md", ".gitignore")

                trimTrailingWhitespace()
                indentWithSpaces()
                endWithNewline()
            }
        }

        if (project === rootProject) {
            extensions.getByType<SpotlessExtension>().predeclareDeps()
            extensions.configure<SpotlessExtensionPredeclare>(configureSpotless)
        } else {
            extensions.configure(configureSpotless)
        }
    }
}

subprojects {
    afterEvaluate {
        plugins.withType<com.android.build.gradle.api.AndroidBasePlugin> {
            apply(plugin = libs.plugins.cache.fix.get().pluginId)
        }

        extensions.findByType(CommonExtension::class.java)?.apply {
            compileOptions {
                sourceCompatibility(JavaVersion.VERSION_17)
                targetCompatibility(JavaVersion.VERSION_17)
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions.apply {
                jvmTarget = "17"
                freeCompilerArgs = buildList {
                    addAll(freeCompilerArgs)

                    add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")

                    add("-P")
                    add("plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=$rootDir/compose_compiler_config.conf")

                    if (project.findProperty("composeCompilerReports") == "true") {
                        val composeCompilerPath = "${project.layout.buildDirectory.asFile.get()}/compose_compiler"
                        add("-P")
                        add("plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeCompilerPath")
                        add("-P")
                        add("plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeCompilerPath")
                    }
                }
            }
        }

        tasks.findByName("preBuild")?.dependsOn("spotlessCheck")
    }
}
