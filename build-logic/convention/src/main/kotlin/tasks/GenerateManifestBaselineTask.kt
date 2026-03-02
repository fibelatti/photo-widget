package tasks

import java.io.File
import java.util.SortedSet
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates a baseline containing all permissions declared in an `AndroidManifest.xml` file.
 *
 * To generate the initial baseline, or after intentional permission changes:
 *
 * ```sh
 * ./gradlew generateDebugManifestBaseline
 * ./gradlew generateReleaseManifestBaseline
 * ```
 */
abstract class GenerateManifestBaselineTask : DefaultTask() {

    /**
     * The merged `AndroidManifest.xml` file to analyze.
     *
     * Usually obtained via `variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)`.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    /**
     * Where to write the baseline, e.g. <root>/manifest-baselines/debug-permissions.txt
     */
    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val manifestFile: File = mergedManifest.get().asFile
        val outputFile: File = baselineFile.get().asFile

        // If a baseline already exists, extract its comments to preserve the documentation
        val permissions: Collection<ManifestPermission> = if (outputFile.exists()) {
            val currentPermissions: SortedSet<ManifestPermission> = PermissionsBaselineParser.parseFile(outputFile)

            PermissionsBaselineParser.parseFile(manifestFile).map { perm: ManifestPermission ->
                currentPermissions.find { it.name == perm.name }
                    ?.let { perm.copy(comment = it.comment) }
                    ?: perm
            }
        } else {
            PermissionsBaselineParser.parseFile(manifestFile)
        }

        outputFile.parentFile.mkdirs()

        PermissionsBaselineParser.generateBaseline(permissions = permissions, outputFile = outputFile)

        logger.lifecycle("✅ Manifest baseline written to: ${outputFile.path}")
        logger.lifecycle("   Tracked permissions (${permissions.size}):")
        permissions.forEach { logger.lifecycle("     ${it.toLogString()}") }
    }
}
