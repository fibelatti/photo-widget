package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class TapActionArea(
    @StringRes val label: Int,
) {

    LEFT(label = R.string.photo_widget_configure_tap_action_area_left),
    CENTER(label = R.string.photo_widget_configure_tap_action_area_center),
    RIGHT(label = R.string.photo_widget_configure_tap_action_area_right),
}
