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
) : Parcelable
