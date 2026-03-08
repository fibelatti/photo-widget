import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("Unused")
class AndroidCommonPlugin : Plugin<Project> {

    private val javaVersion = JavaVersion.VERSION_21

    override fun apply(target: Project) {
        with(target) {
            applyCommonPlugins()
            configureVersions()
            configureKotlin()
            configureCompose()
            configureCoreLibraryDesugaring()
        }
    }

    private fun Project.applyCommonPlugins() {
        plugins.withType<com.android.build.gradle.api.AndroidBasePlugin> {
            apply(plugin = versionCatalog.findPlugin("cache-fix").get().get().pluginId)
        }

        apply(plugin = versionCatalog.findPlugin("fibelatti-spotless").get().get().pluginId)
    }

    private fun Project.configureVersions() {
        val compileSdkVersion: Int by this
        val minSdkVersion: Int by this

        extensions.findByType(CommonExtension::class.java)?.apply {
            compileSdk = compileSdkVersion

            defaultConfig {
                minSdk = minSdkVersion
            }

            compileOptions {
                sourceCompatibility(javaVersion)
                targetCompatibility(javaVersion)
            }
        }
    }

    private fun Project.configureKotlin() {
        apply(plugin = versionCatalog.findPlugin("kotlin-android").get().get().pluginId)

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
                freeCompilerArgs.set(
                    buildList {
                        addAll(freeCompilerArgs.get())
                        add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                        add("-Xannotation-default-target=param-property")
                    },
                )
            }
        }

        dependencies {
            "implementation"(versionCatalog.findLibrary("kotlin").get())
            "implementation"(versionCatalog.findLibrary("coroutines-core").get())
        }
    }

    private fun Project.configureCompose() {
        apply(plugin = versionCatalog.findPlugin("compose-compiler").get().get().pluginId)

        extensions.findByType(CommonExtension::class.java)?.apply {
            buildFeatures {
                compose = true
            }
        }

        extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))

            if (project.findProperty("composeCompilerReports") == "true") {
                val destinationDir = project.layout.buildDirectory.dir("compose_compiler")
                reportsDestination.set(destinationDir)
                metricsDestination.set(destinationDir)
            }
        }

        dependencies {
            val bom = versionCatalog.findLibrary("compose-bom").get()

            "implementation"(platform(bom))
            "implementation"(versionCatalog.findLibrary("compose-runtime").get())
            "implementation"(versionCatalog.findLibrary("compose-material3").get())
            "implementation"(versionCatalog.findLibrary("compose-ui").get())
            "implementation"(versionCatalog.findLibrary("compose-ui-tooling-preview").get())
            "debugImplementation"(versionCatalog.findLibrary("compose-ui-tooling").get())
        }
    }

    private fun Project.configureCoreLibraryDesugaring() {
        extensions.findByType(CommonExtension::class.java)?.apply {
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            "coreLibraryDesugaring"(versionCatalog.findLibrary("core-library-desugaring").get())
        }
    }
}
