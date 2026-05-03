package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import coil3.size.Size
import coil3.transform.Transformation
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.borderPercent

class PolygonalShapeTransformation(
    private val context: Context,
    private val shapeId: String,
    private val colors: PhotoWidgetColors,
    private val border: PhotoWidgetBorder,
    private val resolvedDynamicBorderColor: Int?,
) : Transformation() {

    override val cacheKey: String = "polygonal|$shapeId|$colors|$border|$resolvedDynamicBorderColor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return input.withPolygonalShape(
            context = context,
            shapeId = shapeId,
            colors = colors,
            borderColor = resolveBorderColor(border = border, source = input, dynamic = resolvedDynamicBorderColor),
            borderPercent = border.borderPercent(),
        )
    }

    override fun equals(other: Any?): Boolean = other is PolygonalShapeTransformation && cacheKey == other.cacheKey

    override fun hashCode(): Int = cacheKey.hashCode()
}

class RoundedCornersTransformation(
    private val aspectRatio: PhotoWidgetAspectRatio,
    private val radius: Float,
    private val colors: PhotoWidgetColors,
    private val border: PhotoWidgetBorder,
    private val resolvedDynamicBorderColor: Int?,
) : Transformation() {

    override val cacheKey: String = "rounded|$aspectRatio|$radius|$colors|$border|$resolvedDynamicBorderColor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return input.withRoundedCorners(
            radius = radius,
            aspectRatio = aspectRatio,
            colors = colors,
            borderColor = resolveBorderColor(border = border, source = input, dynamic = resolvedDynamicBorderColor),
            borderPercent = border.borderPercent(),
        )
    }

    override fun equals(other: Any?): Boolean = other is RoundedCornersTransformation && cacheKey == other.cacheKey

    override fun hashCode(): Int = cacheKey.hashCode()
}

@ColorInt
private fun resolveBorderColor(
    border: PhotoWidgetBorder,
    source: Bitmap,
    @ColorInt dynamic: Int?,
): Int? = when (border) {
    is PhotoWidgetBorder.None -> null
    is PhotoWidgetBorder.Color -> "#${border.colorHex}".toColorInt()
    is PhotoWidgetBorder.Dynamic -> dynamic
    is PhotoWidgetBorder.MatchPhoto -> getColorPalette(source).colorForType(border.type)
}
