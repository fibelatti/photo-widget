package com.fibelatti.photowidget.platform

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import com.fibelatti.photowidget.model.PhotoWidgetBorder

class ColorPalette(
    @ColorInt val dominantColor: Int,
    @ColorInt val vibrantColor: Int,
    @ColorInt val mutedColor: Int,
)

@ColorInt
fun ColorPalette.colorForType(type: PhotoWidgetBorder.MatchPhoto.Type): Int {
    return when (type) {
        PhotoWidgetBorder.MatchPhoto.Type.DOMINANT -> dominantColor
        PhotoWidgetBorder.MatchPhoto.Type.VIBRANT -> vibrantColor
        PhotoWidgetBorder.MatchPhoto.Type.MUTED -> mutedColor
    }
}

fun getColorPalette(bitmap: Bitmap): ColorPalette {
    val palette = Palette.from(bitmap).generate()

    return ColorPalette(
        dominantColor = palette.getDominantColor(Color.BLACK),
        vibrantColor = palette.getVibrantColor(Color.BLACK),
        mutedColor = palette.getMutedColor(Color.BLACK),
    )
}
