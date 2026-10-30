package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The font used to render the widget text.
 */
sealed interface PhotoWidgetTextFont : Parcelable {

    /** The system default font. */
    @Parcelize
    data object Default : PhotoWidgetTextFont

    /** A font from the Google Fonts catalog, identified by its [family] name. */
    @Parcelize
    data class GoogleFont(val family: String) : PhotoWidgetTextFont

    /**
     * A font file chosen by the user, kept under its original [fileName]. [path] is the location
     * of the file: a staging copy while the widget is being configured, and the copy in the widget
     * directory once it is saved.
     */
    @Parcelize
    data class Custom(val fileName: String, val path: String) : PhotoWidgetTextFont
}
