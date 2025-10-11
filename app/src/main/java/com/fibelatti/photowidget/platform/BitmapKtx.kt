package com.fibelatti.photowidget.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.rawAspectRatio
import kotlin.math.min
import timber.log.Timber

fun Bitmap.withRoundedCorners(
    radius: Float,
    aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.ROUNDED_SQUARE,
    colors: PhotoWidgetColors = PhotoWidgetColors(),
    @ColorInt borderColor: Int? = null,
    @FloatRange(from = 0.0) borderPercent: Float = .0F,
): Bitmap = withTransformation(
    aspectRatio = aspectRatio,
    colors = colors,
    borderColor = borderColor,
    borderPercent = borderPercent,
) { canvas: Canvas, rect: Rect, paint: Paint ->
    canvas.drawRoundRect(rect.toRectF(), radius, radius, paint)
}

fun Bitmap.withPolygonalShape(
    context: Context,
    shapeId: String,
    colors: PhotoWidgetColors = PhotoWidgetColors(),
    @ColorInt borderColor: Int? = null,
    @FloatRange(from = 0.0) borderPercent: Float = .0F,
): Bitmap = withTransformation(
    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
    colors = colors,
    borderColor = borderColor,
    borderPercent = borderPercent,
) { canvas: Canvas, rect: Rect, paint: Paint ->
    try {
        val path: Path = PhotoWidgetShapeBuilder.getShapePath(
            shapeId = shapeId,
            size = min(rect.height(), rect.width()).toFloat(),
        )

        canvas.drawPath(path, paint)
    } catch (cause: Exception) {
        val wrapped = RuntimeException(
            buildString {
                append("Unable to create shape with `withPolygonalShape`! (")
                append("shapeId=$shapeId,")
                append("bitmap=[$width;$height],")
                append("rect=[${rect.width()};${rect.height()}]")
                append(")")
            },
            cause,
        )

        entryPoint<PhotoWidgetEntryPoint>(context)
            .exceptionReporter()
            .collectReport(throwable = wrapped)

        throw wrapped
    }
}

private inline fun Bitmap.withTransformation(
    aspectRatio: PhotoWidgetAspectRatio,
    colors: PhotoWidgetColors,
    @ColorInt borderColor: Int?,
    @FloatRange(from = 0.0) borderPercent: Float,
    body: (Canvas, Rect, Paint) -> Unit,
): Bitmap {
    val source: Rect = sourceRect(aspectRatio = aspectRatio)
    val destination = Rect(
        /* left = */ 0,
        /* top = */ 0,
        /* right = */ source.width().coerceForAspectRatio(aspectRatio),
        /* bottom = */ source.height().coerceForAspectRatio(aspectRatio),
    )

    val output: Bitmap = createBitmap(destination.width(), destination.height())
    val canvas: Canvas = Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
    }
    val basePaint: Paint = Paint().apply {
        isAntiAlias = true
        alpha = (colors.opacity * 255 / 100).toInt()
    }

    body(canvas, destination, basePaint)

    val bitmapPaint: Paint = Paint(basePaint).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val brightness = colors.brightness * 255 / 100
        val brightnessMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f,
        )

        val colorMatrix = ColorMatrix().apply {
            setSaturation(colors.saturation / 100)
            postConcat(ColorMatrix(brightnessMatrix))
        }
        val colorFilter = ColorMatrixColorFilter(colorMatrix)

        setColorFilter(colorFilter)
    }

    canvas.drawBitmap(this, source, destination, bitmapPaint)

    if (borderColor != null && borderPercent > 0) {
        val strokePaint = Paint(basePaint).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = min(output.width, output.height) * borderPercent
        }
        body(canvas, destination, strokePaint)
    }

    return output
}

private fun Bitmap.sourceRect(aspectRatio: PhotoWidgetAspectRatio): Rect {
    Timber.d(
        "Calculating source rect for bitmap (" +
            "width=$width," +
            "height=$height," +
            "aspectRatio=$aspectRatio," +
            ")",
    )

    return createCenteredRectWithAspectRatio(
        bitmapWidth = width.toFloat(),
        bitmapHeight = height.toFloat(),
        aspectRatio = when (aspectRatio) {
            PhotoWidgetAspectRatio.ORIGINAL, PhotoWidgetAspectRatio.FILL_WIDGET -> width / height.toFloat()
            else -> aspectRatio.rawAspectRatio
        },
    ).toRect().also { Timber.d("Output rect: $it") }
}

/**
 * Creates a centered rectangle with the specified aspect ratio inside a bitmap
 *
 * @param bitmapWidth Width of the bitmap
 * @param bitmapHeight Height of the bitmap
 * @param aspectRatio Width/height ratio the created rect should maintain
 * @return A RectF centered in the bitmap with the specified aspect ratio
 */
private fun createCenteredRectWithAspectRatio(
    bitmapWidth: Float,
    bitmapHeight: Float,
    aspectRatio: Float,
): RectF {
    val bitmapAspectRatio = bitmapWidth / bitmapHeight

    val rectWidth: Float
    val rectHeight: Float

    // Determine if we should fit by width or height
    if (aspectRatio > bitmapAspectRatio) {
        // Width constrained
        rectWidth = bitmapWidth
        rectHeight = rectWidth / aspectRatio
    } else {
        // Height constrained
        rectHeight = bitmapHeight
        rectWidth = rectHeight * aspectRatio
    }

    // Calculate top-left position to center the rect
    val left = (bitmapWidth - rectWidth) / 2
    val top = (bitmapHeight - rectHeight) / 2

    return RectF(left, top, left + rectWidth, top + rectHeight)
}

/**
 * This function establishes a safe dimension for the shapes library.
 *
 * Certain shapes can throw an exception when trying to draw something larger than this.
 */
private fun Int.coerceForAspectRatio(aspectRatio: PhotoWidgetAspectRatio): Int {
    return if (PhotoWidgetAspectRatio.SQUARE != aspectRatio) this else coerceAtMost(500)
}
