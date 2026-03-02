import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register
import tasks.CheckManifestPermissionsTask
import tasks.GenerateManifestBaselineTask

@Suppress("Unused")
class ManifestPermissionValidationPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val androidComponents = target.extensions
            .findByType(AndroidComponentsExtension::class.java)
            ?: error("The ManifestPermissionValidationPlugin requires the Android plugin to be applied first.")

        androidComponents.onVariants { variant ->
            registerTasks(
                target = target,
                variant = variant,
            )
        }
    }

    private fun registerTasks(target: Project, variant: Variant) {
        val variantName: String = variant.name.replaceFirstChar { it.uppercaseChar() }

        // Resolved lazily, Gradle handles the dependency on processXxxManifest
        val mergedManifest: Provider<RegularFile> = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val baselineFile: RegularFile = target.layout.projectDirectory
            .file("manifest-baselines/${variant.name}-permissions.xml")

        target.tasks.register<GenerateManifestBaselineTask>(
            "generate${variantName}ManifestBaseline",
        ) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Writes current '${variant.name}' manifest permissions to the baseline file."

            this.mergedManifest.set(mergedManifest)
            this.baselineFile.set(baselineFile)
        }

        val validateTask = target.tasks.register<CheckManifestPermissionsTask>(
            "check${variantName}ManifestPermissions",
        ) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Validates '${variant.name}' manifest permissions against the baseline file."

            this.mergedManifest.set(mergedManifest)
            this.baselineFile.set(baselineFile)
        }

        target.tasks.named(JavaBasePlugin.CHECK_TASK_NAME) {
            dependsOn(validateTask)
        }
    }
}
