package com.fibelatti.photowidget.preferences

import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction

data class UserPreferences(
    val dataSaver: Boolean,
    val appearance: Appearance,
    val useTrueBlack: Boolean,
    val dynamicColors: Boolean,
    val defaultSource: PhotoWidgetSource,
    val defaultShuffle: Boolean,
    val defaultCycleMode: PhotoWidgetCycleMode,
    val defaultShape: String,
    val defaultCornerRadius: Int,
    val defaultOpacity: Float,
    val defaultSaturation: Float,
    val defaultBrightness: Float,
    val defaultTapAction: PhotoWidgetTapAction,
)
