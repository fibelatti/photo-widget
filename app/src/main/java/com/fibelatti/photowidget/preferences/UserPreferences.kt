package com.fibelatti.photowidget.preferences

import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource

data class UserPreferences(
    val dataSaver: Boolean,
    val appearance: Appearance,
    val useTrueBlack: Boolean,
    val dynamicColors: Boolean,
    val defaultSource: PhotoWidgetSource,
    val defaultShuffle: Boolean,
    val defaultDirectorySorting: DirectorySorting,
    val defaultCycleMode: PhotoWidgetCycleMode,
    val defaultShape: String,
    val defaultCornerRadius: Int,
    val defaultOpacity: Float,
    val defaultSaturation: Float,
    val defaultBrightness: Float,
)
