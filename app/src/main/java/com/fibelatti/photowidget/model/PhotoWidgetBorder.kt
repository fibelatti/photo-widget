package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetBorder : Parcelable {

    fun getBorderWidth(): Int = when (this) {
        is None -> 0
        is Color -> width
        is Dynamic -> width
    }

    @Parcelize
    data object None : PhotoWidgetBorder

    @Parcelize
    data class Color(val colorHex: String, val width: Int) : PhotoWidgetBorder

    @Parcelize
    data class Dynamic(val width: Int) : PhotoWidgetBorder
}
