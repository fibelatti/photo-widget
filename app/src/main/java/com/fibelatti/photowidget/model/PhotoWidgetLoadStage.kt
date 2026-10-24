package com.fibelatti.photowidget.model

/**
 * How much of a [PhotoWidget] has been loaded. Stages are declared in the order they are loaded,
 * but a load can skip stages; Compare them with `>=` or `<` rather than checking for a specific
 * stage.
 */
enum class PhotoWidgetLoadStage {

    /** The widget properties are loaded, but none of its photos. */
    METADATA,

    /**
     * [PhotoWidget.currentPhoto] is loaded from the local photo cache, or null for transparent
     * widgets, while [PhotoWidget.photos] and [PhotoWidget.removedPhotos] are still empty.
     */
    CURRENT_PHOTO,

    /** All photos are loaded from the local photo cache. */
    CACHED_PHOTOS,

    /** All photos are loaded from the widget source. */
    SOURCE_PHOTOS,
}
