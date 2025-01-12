package com.fibelatti.photowidget.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.annotation.IntRange
import androidx.core.graphics.toRectF
import androidx.graphics.shapes.toPath
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.transformed
import kotlin.math.min
import kotlin.math.roundToInt

fun Bitmap.withRoundedCorners(
    aspectRatio: PhotoWidgetAspectRatio,
    radius: Float = PhotoWidget.DEFAULT_CORNER_RADIUS,
    opacity: Float = PhotoWidget.DEFAULT_OPACITY,
    blackAndWhite: Boolean = false,
    borderColorHex: String? = null,
    @IntRange(from = 0) borderWidth: Int = 0,
): Bitmap = withTransformation(
    aspectRatio = aspectRatio,
    opacity = opacity,
    blackAndWhite = blackAndWhite,
    borderColorHex = borderColorHex,
    borderWidth = borderWidth,
) { canvas, rect, paint ->
    canvas.drawRoundRect(rect.toRectF(), radius, radius, paint)
}

fun Bitmap.withPolygonalShape(
    shapeId: String,
    opacity: Float = PhotoWidget.DEFAULT_OPACITY,
    blackAndWhite: Boolean = false,
    borderColorHex: String? = null,
    @IntRange(from = 0) borderWidth: Int = 0,
): Bitmap = withTransformation(
    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
    opacity = opacity,
    blackAndWhite = blackAndWhite,
    borderColorHex = borderColorHex,
    borderWidth = borderWidth,
) { canvas, rect, paint ->
    try {
        val shape = PhotoWidgetShapeBuilder.buildShape(
            shapeId = shapeId,
            width = width.toFloat(),
            height = height.toFloat(),
        )

        canvas.drawPath(shape.transformed(bounds = rect.toRectF()).toPath(), paint)
    } catch (e: Exception) {
        val message = "withPolygonalShape failed! " +
            "(shapeId=$shapeId, bitmap=$width, $height, rect=${rect.width()}, ${rect.height()})"

        throw RuntimeException(message, e)
    }
}

private inline fun Bitmap.withTransformation(
    aspectRatio: PhotoWidgetAspectRatio,
    opacity: Float,
    blackAndWhite: Boolean,
    borderColorHex: String?,
    @IntRange(from = 0) borderWidth: Int,
    body: (Canvas, Rect, Paint) -> Unit,
): Bitmap {
    val source = when (aspectRatio) {
        PhotoWidgetAspectRatio.SQUARE -> {
            val size = min(height, width)

            val top = (height - size) / 2
            val left = (width - size) / 2

            Rect(left, top, left + size, top + size)
        }

        PhotoWidgetAspectRatio.TALL -> {
            val baseWidth = (height * PhotoWidgetAspectRatio.TALL.scale)

            val scaledWidth = baseWidth.roundToInt().coerceAtMost(width)
            val scaledHeight = if (baseWidth > width) {
                ((width / baseWidth) * height).roundToInt()
            } else {
                height
            }

            val top = (height - scaledHeight) / 2
            val left = (width - scaledWidth) / 2

            Rect(left, top, left + scaledWidth, top + scaledHeight)
        }

        PhotoWidgetAspectRatio.WIDE -> {
            val baseHeight = (width * PhotoWidgetAspectRatio.WIDE.scale)

            val scaledHeight = baseHeight.roundToInt().coerceAtMost(height)
            val scaledWidth = if (baseHeight > height) {
                ((height / baseHeight) * width).roundToInt()
            } else {
                width
            }

            val top = (height - scaledHeight) / 2
            val left = (width - scaledWidth) / 2

            Rect(left, top, left + scaledWidth, top + scaledHeight)
        }

        PhotoWidgetAspectRatio.ORIGINAL, PhotoWidgetAspectRatio.FILL_WIDGET -> {
            Rect(0, 0, width, height)
        }
    }
    val destination = Rect(0, 0, source.width(), source.height())

    val output = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply {
        isAntiAlias = true
        alpha = (opacity * 255 / 100).toInt()
    }

    canvas.drawARGB(0, 0, 0, 0)

    body(canvas, if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) source else destination, paint)

    paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))

    val bitmapPaint = Paint(paint)

    if (blackAndWhite) {
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        bitmapPaint.setColorFilter(colorFilter)
    }

    canvas.drawBitmap(this, source, destination, bitmapPaint)

    val borderColor = runCatching { Color.parseColor("#$borderColorHex") }.getOrNull()
    if (borderColor != null && borderWidth > 0) {
        val stroke = Paint(paint).apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = borderWidth.toFloat()
        }
        body(canvas, if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) source else destination, stroke)
    }

    return output
}
