import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.cache.fix) apply false
    alias(libs.plugins.about.libraries) apply false
    alias(libs.plugins.licensee) apply false

    alias(libs.plugins.fibelatti.spotless)
}

buildscript {
    extra["compileSdkVersion"] = 36
    extra["targetSdkVersion"] = 36
    extra["minSdkVersion"] = 26
}

val javaVersion = JavaVersion.VERSION_21

subprojects {
    afterEvaluate {
        plugins.withType<com.android.build.gradle.api.AndroidBasePlugin> {
            apply(plugin = libs.plugins.cache.fix.get().pluginId)
        }

        extensions.findByType(CommonExtension::class.java)?.apply {
            val compileSdkVersion: Int by project
            val minSdkVersion: Int by project

            compileSdk = compileSdkVersion

            defaultConfig {
                minSdk = minSdkVersion
            }

            compileOptions {
                isCoreLibraryDesugaringEnabled = true

                sourceCompatibility(javaVersion)
                targetCompatibility(javaVersion)
            }
        }

        extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))

            if (project.findProperty("composeCompilerReports") == "true") {
                val destinationDir = project.layout.buildDirectory.dir("compose_compiler")
                reportsDestination = destinationDir
                metricsDestination = destinationDir
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                freeCompilerArgs = buildList {
                    addAll(freeCompilerArgs.get())
                    add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                    add("-Xannotation-default-target=param-property")
                }
            }
        }

        dependencies {
            "coreLibraryDesugaring"(libs.core.library.desugaring)
        }
    }
}
