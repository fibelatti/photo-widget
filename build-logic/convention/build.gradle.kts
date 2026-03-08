import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.fibelatti.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidCommon") {
            id = libs.plugins.fibelatti.android.common.get().pluginId
            implementationClass = "AndroidCommonPlugin"
        }

        register("manifestPermissionValidation") {
            id = libs.plugins.fibelatti.manifest.permission.validation.get().pluginId
            implementationClass = "ManifestPermissionValidationPlugin"
        }

        register("spotless") {
            id = libs.plugins.fibelatti.spotless.get().pluginId
            implementationClass = "SpotlessPlugin"
        }
    }
}
