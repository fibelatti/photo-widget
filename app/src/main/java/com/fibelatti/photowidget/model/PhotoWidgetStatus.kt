package com.fibelatti.photowidget.model

enum class PhotoWidgetStatus {

    /**
     * The widget is currently added to a home screen.
     */
    ACTIVE,

    /**
     * The widget is currently added to a home screen, but the current photo is locked.
     */
    LOCKED,

    /**
     * The widget was removed from a home screen and will be permanently deleted soon.
     */
    REMOVED,

    /**
     * The widget was removed from a home screen but won't be deleted from the app unless the user
     * chooses to.
     */
    KEPT,
}

val PhotoWidgetStatus.isWidgetRemoved: Boolean
    get() = PhotoWidgetStatus.REMOVED == this || PhotoWidgetStatus.KEPT == this
