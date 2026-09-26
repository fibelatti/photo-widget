package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.borderPercent
import com.fibelatti.photowidget.model.resolveColor

class PolygonalShapeTransformation(
    private val context: Context,
    private val shapeId: String,
    private val shapeRotation: Int,
    private val colors: PhotoWidgetColors,
    private val border: PhotoWidgetBorder,
) : Transformation() {

    override val cacheKey: String =
        "polygonal|$shapeId|$shapeRotation|$colors|$border"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return input.withPolygonalShape(
            context = context,
            shapeId = shapeId,
            shapeRotation = shapeRotation,
            colors = colors,
            borderColor = border.resolveColor(context = context, colorPalette = { getColorPalette(input) }),
            borderPercent = border.borderPercent(),
        )
    }

    override fun equals(other: Any?): Boolean = other is PolygonalShapeTransformation && cacheKey == other.cacheKey

    override fun hashCode(): Int = cacheKey.hashCode()
}

class RoundedCornersTransformation(
    private val context: Context,
    private val aspectRatio: PhotoWidgetAspectRatio,
    private val radius: Float,
    private val colors: PhotoWidgetColors,
    private val border: PhotoWidgetBorder,
) : Transformation() {

    override val cacheKey: String = "rounded|$aspectRatio|$radius|$colors|$border"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return input.withRoundedCorners(
            radius = radius,
            aspectRatio = aspectRatio,
            colors = colors,
            borderColor = border.resolveColor(context = context, colorPalette = { getColorPalette(input) }),
            borderPercent = border.borderPercent(),
        )
    }

    override fun equals(other: Any?): Boolean = other is RoundedCornersTransformation && cacheKey == other.cacheKey

    override fun hashCode(): Int = cacheKey.hashCode()
}
