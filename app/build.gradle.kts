@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.about.libraries)
    alias(libs.plugins.licensee)
}

object AppInfo {

    const val APP_NAME = "Material Photo Widget"
    const val APPLICATION_ID = "com.fibelatti.photowidget"

    private const val VERSION_MAJOR = 1
    private const val VERSION_MINOR = 34
    private const val VERSION_PATCH = 0
    private const val VERSION_BUILD = 0

    val versionCode: Int = (VERSION_MAJOR * 1_000_000)
        .plus(VERSION_MINOR * 10_000)
        .plus(VERSION_PATCH * 100)
        .plus(VERSION_BUILD)
        .also { println("versionCode: $it") }

    @Suppress("KotlinConstantConditions")
    val versionName: String = StringBuilder("$VERSION_MAJOR.$VERSION_MINOR")
        .apply { if (VERSION_PATCH != 0) append(".$VERSION_PATCH") }
        .toString()
        .also { println("versionName: $it") }
}

android {
    val compileSdkVersion: Int by project
    val targetSdkVersion: Int by project
    val minSdkVersion: Int by project

    namespace = "com.fibelatti.photowidget"
    compileSdk = compileSdkVersion

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = AppInfo.APPLICATION_ID
        versionCode = AppInfo.versionCode
        versionName = AppInfo.versionName
        targetSdk = targetSdkVersion
        minSdk = minSdkVersion

        base.archivesName = "$applicationId-v$versionName-$versionCode"

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        getByName("debug") {
            storeFile = File("$rootDir/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("release") {
            storeFile = File("$rootDir/keystore/release.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false

            if (System.getenv("SIGN_BUILD").toBoolean()) {
                signingConfig = signingConfigs.getByName("release")
            }

            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    File("proguard-rules.pro"),
                ),
            )
        }
    }

    androidComponents {
        onVariants { variant ->
            val appName = StringBuilder().apply {
                append(AppInfo.APP_NAME)
                if (variant.name.contains("debug", ignoreCase = true)) append(" Dev")
            }.toString()

            variant.resValues.put(
                variant.makeResValueKey("string", "app_name"),
                com.android.build.api.variant.ResValue(appName, null),
            )

            variant.androidResources.localeFilters
                .addAll("en", "de", "es", "fr", "it", "iw", "ja", "pt", "ru", "tl", "tr")
        }
    }

    sourceSets {
        forEach { sourceSet -> getByName(sourceSet.name).java.srcDirs("src/${sourceSet.name}/kotlin") }
    }

    packaging {
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    lint {
        warningsAsErrors = true
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

room {
    schemaDirectory("$projectDir/schemas")
}

aboutLibraries {
    collect {
        filterVariants.add("release")
    }

    export {
        variant = "release"
        excludeFields = setOf("generated")
    }

    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.EXACT
    }
}

licensee {
    allow("Apache-2.0")
}

dependencies {
    implementation(projects.ui)

    // Kotlin
    implementation(libs.kotlin)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.datetime)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Android Platform
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.material)
    implementation(libs.graphics.shapes)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.palette)
    implementation(libs.work.runtime.ktx)

    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Misc
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    implementation(libs.dagger.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.work)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(libs.image.cropper)
    implementation(libs.coil)
    implementation(libs.reorderable)
    implementation(libs.colorpicker.compose)
    implementation(libs.zoomable)

    implementation(libs.about.libraries)

    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    lintChecks(libs.compose.lint.checks)
}

/**
 * Prints the current version code. Used for GitHub releases.
 */
val printReleaseVersionCode by tasks.registering {
    doLast {
        println(AppInfo.versionCode)
    }
}
