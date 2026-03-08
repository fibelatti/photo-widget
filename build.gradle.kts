plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
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
