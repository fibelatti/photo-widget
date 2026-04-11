import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

@Suppress("Unused")
class SpotlessPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.diffplug.spotless")

            if (target === rootProject) {
                extensions.getByType<SpotlessExtension>().predeclareDeps()
                extensions.configure<SpotlessExtensionPredeclare> { configureSpotless() }
            } else {
                extensions.configure<SpotlessExtension> { configureSpotless() }
            }
        }
    }

    context(target: Project)
    private fun SpotlessExtension.configureSpotless() {
        val allRules: Map<String, Any> = getRules()

        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")

            ktlint("1.8.0")
                .setEditorConfigPath("${target.rootProject.projectDir}/.editorconfig")
                .editorConfigOverride(allRules)
                .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:0.5.7"))

            trimTrailingWhitespace()
            leadingTabsToSpaces()
            endWithNewline()
        }
        kotlinGradle {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")

            ktlint("1.8.0")
                .setEditorConfigPath("${target.rootProject.projectDir}/.editorconfig")
                .editorConfigOverride(allRules)

            trimTrailingWhitespace()
            leadingTabsToSpaces()
            endWithNewline()
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
        }
        format("misc") {
            target("*.gradle", "*.md", ".gitignore")

            trimTrailingWhitespace()
            leadingTabsToSpaces()
            endWithNewline()
        }
    }

    private fun getRules(): Map<String, Any> {
        val configuredRules: Map<String, Any> = mapOf(
            "ktlint_code_style" to "android_studio",
            "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
            "ktlint_ignore_back_ticked_identifier" to true,
            "compose_allowed_composition_locals" to "LocalSamplePhoto",
        )

        val disabledRules: Map<String, String> = listOf(
            "ktlint_standard_comment-wrapping",
            "ktlint_standard_class-signature",
            "ktlint_standard_function-expression-body",
            "ktlint_standard_function-signature",
        ).associateWith { "disabled" }

        return configuredRules + disabledRules
    }
}
