import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("Unused")
class AndroidCommonPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            applyCommonPlugins()
            configureSdkCompatibility()
            configureKotlin()
            configureCompose()
            configureCoreLibraryDesugaring()
        }
    }

    private fun Project.applyCommonPlugins() {
        apply(plugin = versionCatalog.findPluginIdByAlias("cache-fix"))
        apply(plugin = versionCatalog.findPluginIdByAlias("fibelatti-spotless"))
    }

    private fun Project.configureSdkCompatibility() {
        val compileSdkVersion: Int by this
        val minSdkVersion: Int by this

        extensions.getByType<CommonExtension>().apply {
            compileSdk = compileSdkVersion

            defaultConfig.apply {
                minSdk = minSdkVersion
            }

            compileOptions.apply {
                sourceCompatibility(javaVersion)
                targetCompatibility(javaVersion)
            }
        }
    }

    private fun Project.configureKotlin() {
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
            implementation("kotlin")
            implementation("coroutines-core")
        }
    }

    private fun Project.configureCompose() {
        apply(plugin = versionCatalog.findPluginIdByAlias("compose-compiler"))

        extensions.getByType<CommonExtension>().apply {
            buildFeatures.apply {
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

            implementation(platform(bom))
            implementation("compose-runtime")
            implementation("compose-material3")
            implementation("compose-ui")
            implementation("compose-ui-tooling-preview")
            debugImplementation("compose-ui-tooling")
        }
    }

    private fun Project.configureCoreLibraryDesugaring() {
        extensions.getByType<CommonExtension>().apply {
            compileOptions.apply {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            "coreLibraryDesugaring"(versionCatalog.findLibrary("core-library-desugaring").get())
        }
    }
}
