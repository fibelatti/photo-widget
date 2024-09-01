package com.fibelatti.photowidget.preferences

import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction

data class UserPreferences(
    val appearance: Appearance,
    val dynamicColors: Boolean,
    val defaultSource: PhotoWidgetSource,
    val defaultShuffle: Boolean,
    val defaultCycleMode: PhotoWidgetCycleMode,
    val defaultShape: String,
    val defaultCornerRadius: Float,
    val defaultOpacity: Float,
    val defaultTapAction: PhotoWidgetTapAction,
    val defaultIncreaseBrightness: Boolean,
)
