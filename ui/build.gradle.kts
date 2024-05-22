plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    val compileSdkVersion: Int by project
    val minSdkVersion: Int by project

    namespace = "com.fibelatti.ui"
    compileSdk = compileSdkVersion

    buildFeatures {
        compose = true
    }

    defaultConfig {
        minSdk = minSdkVersion
    }
}

dependencies {
    implementation(libs.kotlin)

    implementation(libs.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
