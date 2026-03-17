import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

internal val javaVersion = JavaVersion.VERSION_21

internal val Project.versionCatalog
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.findPluginIdByAlias(alias: String): String {
    return findPlugin(alias).get().get().pluginId
}

context(project: Project)
internal fun DependencyHandlerScope.implementation(alias: String) {
    "implementation"(project.versionCatalog.findLibrary(alias).get())
}

context(project: Project)
internal fun DependencyHandlerScope.debugImplementation(alias: String) {
    "debugImplementation"(project.versionCatalog.findLibrary(alias).get())
}

internal fun DependencyHandlerScope.implementation(provider: Provider<MinimalExternalModuleDependency>) {
    "implementation"(provider)
}
