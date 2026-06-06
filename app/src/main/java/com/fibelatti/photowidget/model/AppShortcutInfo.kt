package com.fibelatti.photowidget.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Stable

@Stable
data class AppShortcutInfo(
    val id: String,
    val label: String,
    val icon: Drawable?,
)
