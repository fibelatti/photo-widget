package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetBorder : Parcelable {

    fun getBorderPercent(): Float = getBorderWidth() * PERCENT_FACTOR

    private fun getBorderWidth(): Int = when (this) {
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

    companion object {

        val VALUE_RANGE: ClosedFloatingPointRange<Float> = 0F..80F

        const val DEFAULT_WIDTH: Int = 40

        /**
         * Calculated based on a 400px image, where 20px was the maximum border width allowed.
         */
        const val PERCENT_FACTOR: Float = .00125F
    }
}
