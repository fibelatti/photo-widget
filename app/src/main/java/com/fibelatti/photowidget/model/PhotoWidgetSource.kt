package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R

/**
 * Describes where the photos from a widget are coming from.
 */
enum class PhotoWidgetSource(
    @StringRes val label: Int,
) {

    /**
     * The user selected each photo individually.
     */
    PHOTOS(label = R.string.photo_widget_source_photos),

    /**
     * The user selected a directory and all its photos are added automatically.
     */
    DIRECTORY(label = R.string.photo_widget_source_directory),
}
