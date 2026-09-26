package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class MatchPhotoColorType(@StringRes val label: Int) {
    DOMINANT(R.string.photo_widget_configure_color_palette_dominant),
    VIBRANT(R.string.photo_widget_configure_color_palette_vibrant),
    MUTED(R.string.photo_widget_configure_color_palette_muted),
}
