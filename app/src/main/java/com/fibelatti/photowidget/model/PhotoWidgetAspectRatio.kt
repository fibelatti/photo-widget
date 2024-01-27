package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import kotlin.math.max
import kotlin.math.min

enum class PhotoWidgetAspectRatio(
    val x: Float,
    val y: Float,
    @StringRes val label: Int,
) {

    SQUARE(
        x = 1f,
        y = 1f,
        label = R.string.photo_widget_aspect_ratio_square,
    ),
    TALL(
        x = 10f,
        y = 16f,
        label = R.string.photo_widget_aspect_ratio_tall,
    ),
    WIDE(
        x = 16f,
        y = 10f,
        label = R.string.photo_widget_aspect_ratio_wide,
    ),
    ;

    val aspectRatio: Float
        get() = x / y

    val scale: Float
        get() = min(x, y) / max(x, y)

    companion object {

        const val DEFAULT_CORNER_RADIUS: Float = 64f
    }
}
