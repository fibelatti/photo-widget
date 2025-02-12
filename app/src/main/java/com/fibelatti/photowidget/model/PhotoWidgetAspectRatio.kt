package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import kotlin.math.max
import kotlin.math.min

enum class PhotoWidgetAspectRatio(
    val x: Float,
    val y: Float,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val isConstrained: Boolean = true,
) {

    SQUARE(
        x = 1f,
        y = 1f,
        label = R.string.photo_widget_aspect_ratio_square,
        description = R.string.photo_widget_aspect_ratio_square_description,
    ),
    TALL(
        x = 10f,
        y = 16f,
        label = R.string.photo_widget_aspect_ratio_tall,
        description = R.string.photo_widget_aspect_ratio_tall_description,
    ),
    WIDE(
        x = 16f,
        y = 10f,
        label = R.string.photo_widget_aspect_ratio_wide,
        description = R.string.photo_widget_aspect_ratio_wide_description,
    ),
    ORIGINAL(
        x = 4f,
        y = 5f,
        label = R.string.photo_widget_aspect_ratio_original,
        description = R.string.photo_widget_aspect_ratio_original_description,
        isConstrained = false,
    ),
    FILL_WIDGET(
        x = 4f,
        y = 3f,
        label = R.string.photo_widget_aspect_ratio_fill_widget,
        description = R.string.photo_widget_aspect_ratio_fill_widget_description,
        isConstrained = false,
    ),
    ;

    val aspectRatio: Float
        get() = x / y

    val scale: Float
        get() = min(x, y) / max(x, y)
}
