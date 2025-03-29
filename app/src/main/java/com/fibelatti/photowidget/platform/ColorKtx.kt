package com.fibelatti.photowidget.platform

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import com.fibelatti.photowidget.model.PhotoWidgetBorder

class ColorPalette(
    @ColorInt val dominantColor: Int,
    @ColorInt val complementaryColor: Int,
    @ColorInt val analogousColor: Int,
)

@ColorInt
fun ColorPalette.colorForType(type: PhotoWidgetBorder.MatchPhoto.Type): Int {
    return when (type) {
        PhotoWidgetBorder.MatchPhoto.Type.MONOCHROMATIC -> dominantColor
        PhotoWidgetBorder.MatchPhoto.Type.COMPLEMENTARY -> complementaryColor
        PhotoWidgetBorder.MatchPhoto.Type.ANALOGOUS -> analogousColor
    }
}

fun getColorPalette(bitmap: Bitmap): ColorPalette {
    val dominantColor = getDominantColor(bitmap)
    val complementaryColor = getComplementaryColor(dominantColor)
    val analogousColor = getAnalogousColors(dominantColor).first()

    return ColorPalette(
        dominantColor = dominantColor,
        complementaryColor = complementaryColor,
        analogousColor = analogousColor,
    )
}

/**
 * Extracts a palette from the given [Bitmap] and returns the [ColorInt] of the dominant swatch.
 *
 * @param bitmap The [Bitmap] used to generate a palette
 * @return The dominant color as [ColorInt]
 */
@ColorInt
fun getDominantColor(bitmap: Bitmap): Int {
    return Palette.from(bitmap).generate().getDominantColor(Color.BLACK)
}

/**
 * Calculates the complementary color for a given [ColorInt].
 *
 * @param sourceColor The original color as [ColorInt]
 * @return The complementary color as [ColorInt]
 */
@ColorInt
fun getComplementaryColor(@ColorInt sourceColor: Int): Int {
    val red = Color.red(sourceColor)
    val green = Color.green(sourceColor)
    val blue = Color.blue(sourceColor)
    val alpha = Color.alpha(sourceColor)

    val complementaryRed = 255 - red
    val complementaryGreen = 255 - green
    val complementaryBlue = 255 - blue

    return Color.argb(alpha, complementaryRed, complementaryGreen, complementaryBlue)
}

/**
 * Calculates analogous colors for a given [ColorInt].
 *
 * @param sourceColor The original color as [ColorInt]
 * @param angle The angle of shift in the color wheel (typically 30 degrees)
 * @return Array with two analogous colors as [ColorInt] values
 */
fun getAnalogousColors(@ColorInt sourceColor: Int, angle: Float = 30f): Array<Int> {
    val hsv = FloatArray(3)
    Color.colorToHSV(sourceColor, hsv)

    val alpha = Color.alpha(sourceColor)

    // Create the two analogous colors by shifting the hue
    val hue = hsv[0]

    // First analogous color (shift to left on color wheel)
    val hsv1 = hsv.clone()
    hsv1[0] = (hue - angle + 360) % 360
    val analogous1 = Color.HSVToColor(alpha, hsv1)

    // Second analogous color (shift to right on color wheel)
    val hsv2 = hsv.clone()
    hsv2[0] = (hue + angle) % 360
    val analogous2 = Color.HSVToColor(alpha, hsv2)

    return arrayOf(analogous1, analogous2)
}
