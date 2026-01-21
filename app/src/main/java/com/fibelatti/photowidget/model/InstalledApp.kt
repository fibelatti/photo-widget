package com.fibelatti.photowidget.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable

@Stable
data class InstalledApp(
    val appPackage: String,
    val appIcon: Drawable,
    val appLabel: String,
)
