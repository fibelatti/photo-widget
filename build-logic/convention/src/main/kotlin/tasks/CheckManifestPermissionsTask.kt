package tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Identifies discrepancies of declared permissions between a given `AndroidManifest.xml` file and a previously
 * generated baseline.
 *
 * Usage:
 * ```sh
 * ./gradlew checkDebugManifestPermissions
 * ./gradlew checkReleaseManifestPermissions
 * ```
 */
abstract class CheckManifestPermissionsTask : DefaultTask() {

    /**
     * The merged `AndroidManifest.xml` file to analyze.
     *
     * Usually obtained via `variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)`.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    /**
     * The previously generated baseline file.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun check() {
        val manifestFile: File = mergedManifest.get().asFile
        val baselineFile: File = baselineFile.get().asFile

        if (!baselineFile.exists()) {
            throw GradleException(
                "Baseline file not found: ${baselineFile.absolutePath}.\nHave you generated one first?",
            )
        }

        val baselinePermissions: Set<ManifestPermission> = PermissionsBaselineParser.parseFile(baselineFile)
        val currentPermissions: Set<ManifestPermission> = PermissionsBaselineParser.parseFile(manifestFile)

        val baselineNames: Set<Pair<ManifestTag, String>> = baselinePermissions.map { it.tag to it.name }.toSet()
        val currentNames: Set<Pair<ManifestTag, String>> = currentPermissions.map { it.tag to it.name }.toSet()

        val violations: MutableList<String> = mutableListOf()

        val addedPermissions = currentNames - baselineNames
        if (addedPermissions.isNotEmpty()) {
            violations += buildAddedPermissionsMessage(addedPermissions)
        }

        val missingPermissions = baselineNames - currentNames
        if (missingPermissions.isNotEmpty()) {
            violations += buildMissingPermissionsMessage(missingPermissions)
        }

        val changedPermissions = currentPermissions.mapNotNull { current ->
            val expected = baselinePermissions.find { it.tag == current.tag && it.name == current.name }
            if (expected != null && current.protectionLevel != expected.protectionLevel) {
                current.name to (expected.protectionLevel to current.protectionLevel)
            } else {
                null
            }
        }

        if (changedPermissions.isNotEmpty()) {
            violations += buildChangedPermissionsMessage(changedPermissions)
        }

        if (violations.isNotEmpty()) {
            throw GradleException(buildErrorMessage(violations))
        }

        logger.lifecycle("✅ Manifest permissions match baseline.")
    }

    private fun buildAddedPermissionsMessage(permissions: Set<Pair<ManifestTag, String>>): String {
        return buildString {
            appendLine("  🚨 Unexpected permissions (${permissions.size}):")
            permissions.forEach { (tag, name) ->
                appendLine("    + ${tag.manifestValue}: $name")
            }
        }
    }

    private fun buildMissingPermissionsMessage(permissions: Set<Pair<ManifestTag, String>>): String {
        return buildString {
            appendLine("  ⚠️ Removed permissions (${permissions.size}):")
            permissions.forEach { (tag, name) ->
                appendLine("    - ${tag.manifestValue}: $name")
            }
        }
    }

    private fun buildChangedPermissionsMessage(permissions: List<Pair<String, Pair<String?, String?>>>): String {
        return buildString {
            appendLine("  ⚠️ Changed permissions (${permissions.size}):")
            permissions.forEach { (name, levels) ->
                appendLine("    ~ $name: ${levels.first} -> ${levels.second}")
            }
        }
    }

    private fun buildErrorMessage(violations: MutableList<String>): String = buildString {
        appendLine("❌ Manifest permissions differ from baseline!")
        appendLine()

        violations.forEach { appendLine(it) }

        appendLine("  If this change is intentional, regenerate the baseline by running:")

        val variantName: String = name.removePrefix("check").removeSuffix("Permissions")
        appendLine("    ./gradlew generate${variantName}Baseline")
        appendLine()
        appendLine("  If not intentional, investigate the source of the change and fix it.")
    }
}
