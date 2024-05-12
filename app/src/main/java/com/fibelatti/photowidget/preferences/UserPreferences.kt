package com.fibelatti.photowidget.preferences

import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction

data class UserPreferences(
    val appearance: Appearance,
    val dynamicColors: Boolean,
    val defaultSource: PhotoWidgetSource,
    val defaultShuffle: Boolean,
    val defaultIntervalEnabled: Boolean,
    val defaultInterval: PhotoWidgetLoopingInterval,
    val defaultShape: String,
    val defaultCornerRadius: Float,
    val defaultTapAction: PhotoWidgetTapAction,
    val defaultIncreaseBrightness: Boolean,
)
