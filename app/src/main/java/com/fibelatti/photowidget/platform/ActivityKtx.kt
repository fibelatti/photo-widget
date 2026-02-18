package com.fibelatti.photowidget.platform

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Delegates to [enableEdgeToEdge], with a transparent navigation bar style.
 */
fun ComponentActivity.enableEdgeToEdgeTransparent() {
    enableEdgeToEdge(
        navigationBarStyle = SystemBarStyle.auto(
            lightScrim = Color.Transparent.toArgb(),
            darkScrim = Color.Transparent.toArgb(),
        ),
    )
}

/**
 * Disables the navigation bar scrim when running on [Q][Build.VERSION_CODES.Q] or above.
 *
 * Typically, this should only be used in situations where the caller is certain that contrast is
 * already enforced some other way.
 */
fun ComponentActivity.disableWindowNavigationBarContrastEnforced() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
