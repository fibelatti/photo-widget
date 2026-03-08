plugins {
    alias(libs.plugins.android.library)

    alias(libs.plugins.fibelatti.android.common)
}

android {
    namespace = "com.fibelatti.ui"
}

dependencies {
    implementation(libs.core.ktx)
}
