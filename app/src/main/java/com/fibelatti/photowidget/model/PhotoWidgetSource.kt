package com.fibelatti.photowidget.model

/**
 * Describes where the photos from a widget are coming from.
 */
enum class PhotoWidgetSource {

    /**
     * The user selected each photo individually.
     */
    PHOTOS,

    /**
     * The user selected a directory and all its photos are added automatically.
     */
    DIRECTORY,
}
