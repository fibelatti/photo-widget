package com.fibelatti.photowidget.model

import android.os.Parcelable
import com.fibelatti.photowidget.model.PhotoWidget.Companion.DEFAULT_BRIGHTNESS
import com.fibelatti.photowidget.model.PhotoWidget.Companion.DEFAULT_OPACITY
import com.fibelatti.photowidget.model.PhotoWidget.Companion.DEFAULT_SATURATION
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidgetColors(
    val opacity: Float = DEFAULT_OPACITY,
    val saturation: Float = DEFAULT_SATURATION,
    val brightness: Float = DEFAULT_BRIGHTNESS,
) : Parcelable {

    companion object {

        /**
         * Saturation is persisted using a [0..200] range, while the UI expects a [-100..100]
         * range. This function converts the persisted value to the UI value.
         */
        fun pickerSaturation(saturation: Float): Float = saturation - 100

        /**
         * The saturation picker UI uses a [-100..100] range, while the persisted value uses
         * [0..200]. This function converts the UI value to the persisted value.
         */
        fun persistenceSaturation(saturation: Float): Float = saturation + 100
    }
}
