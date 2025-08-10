package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

enum class PhotoWidgetAspectRatio(
    val x: Float,
    val y: Float,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val isConstrained: Boolean = true,
) {

    /**
     * A square aspect ratio that supports shapes, but not rounded corners.
     *
     * The enum name was kept unchanged when [ROUNDED_SQUARE] was introduced for backwards
     * compatibility since it is persisted to local storage.
     */
    SQUARE(
        x = 1f,
        y = 1f,
        label = R.string.photo_widget_aspect_ratio_shape,
        description = R.string.photo_widget_aspect_ratio_shape_description,
    ),

    /**
     * A square aspect ratio that supports rounded corners, but not shapes.
     */
    ROUNDED_SQUARE(
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
}

val PhotoWidgetAspectRatio.rawAspectRatio: Float
    get() = x / y
