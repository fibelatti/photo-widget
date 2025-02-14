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
import android.util.Size
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.toRectF
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import kotlin.math.min
import kotlin.math.roundToInt
import timber.log.Timber

fun Bitmap.withRoundedCorners(
    aspectRatio: PhotoWidgetAspectRatio,
    radius: Float = PhotoWidget.DEFAULT_CORNER_RADIUS,
    opacity: Float = PhotoWidget.DEFAULT_OPACITY,
    blackAndWhite: Boolean = false,
    @ColorInt borderColor: Int? = null,
    @FloatRange(from = 0.0) borderPercent: Float = .0F,
    widgetSize: Size? = null,
): Bitmap = withTransformation(
    aspectRatio = aspectRatio,
    opacity = opacity,
    blackAndWhite = blackAndWhite,
    borderColor = borderColor,
    borderPercent = borderPercent,
    widgetSize = widgetSize,
) { canvas, rect, paint ->
    canvas.drawRoundRect(rect.toRectF(), radius, radius, paint)
}

fun Bitmap.withPolygonalShape(
    shapeId: String,
    opacity: Float = PhotoWidget.DEFAULT_OPACITY,
    blackAndWhite: Boolean = false,
    @ColorInt borderColor: Int? = null,
    @FloatRange(from = 0.0) borderPercent: Float = .0F,
): Bitmap = withTransformation(
    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
    opacity = opacity,
    blackAndWhite = blackAndWhite,
    borderColor = borderColor,
    borderPercent = borderPercent,
    widgetSize = null,
) { canvas, rect, paint ->
    try {
        val path = PhotoWidgetShapeBuilder.getShapePath(
            shapeId = shapeId,
            width = width.toFloat(),
            height = height.toFloat(),
            rectF = rect.toRectF(),
        )

        canvas.drawPath(path, paint)
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
    @ColorInt borderColor: Int?,
    @FloatRange(from = 0.0) borderPercent: Float,
    widgetSize: Size?,
    body: (Canvas, Rect, Paint) -> Unit,
): Bitmap {
    val source = sourceRect(aspectRatio = aspectRatio, widgetSize = widgetSize)
    val destination = Rect(0, 0, source.width(), source.height())

    val output = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
    }
    val basePaint = Paint().apply {
        isAntiAlias = true
        alpha = (opacity * 255 / 100).toInt()
    }

    val bodyRect = if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) source else destination

    body(canvas, bodyRect, basePaint)

    val bitmapPaint = Paint(basePaint).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        if (blackAndWhite) {
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            val colorFilter = ColorMatrixColorFilter(colorMatrix)

            setColorFilter(colorFilter)
        }
    }

    canvas.drawBitmap(this, source, destination, bitmapPaint)

    if (borderColor != null && borderPercent > 0) {
        val strokePaint = Paint(basePaint).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = min(output.width, output.height) * borderPercent
        }
        body(canvas, bodyRect, strokePaint)
    }

    return output
}

private fun Bitmap.sourceRect(
    aspectRatio: PhotoWidgetAspectRatio,
    widgetSize: Size? = null,
): Rect {
    Timber.d(
        "Calculating source rect for bitmap (" +
            "width=$width," +
            "height=$height," +
            "aspectRatio=$aspectRatio," +
            "widgetSize=$widgetSize" +
            ")",
    )

    return when (aspectRatio) {
        PhotoWidgetAspectRatio.SQUARE -> {
            val size = min(height, width)

            val top = (height - size) / 2
            val left = (width - size) / 2

            Rect(left, top, left + size, top + size)
        }

        PhotoWidgetAspectRatio.TALL -> {
            tallRect(width = width, height = height)
        }

        PhotoWidgetAspectRatio.WIDE -> {
            wideRect(width = width, height = height)
        }

        PhotoWidgetAspectRatio.FILL_WIDGET -> {
            when {
                widgetSize == null || widgetSize.width == 0 || widgetSize.height == 0 -> {
                    Rect(0, 0, width, height)
                }

                widgetSize.width > widgetSize.height -> {
                    wideRect(
                        width = width,
                        height = height,
                        scale = widgetSize.height / widgetSize.width.toFloat(),
                    )
                }

                else -> {
                    tallRect(
                        width = width,
                        height = height,
                        scale = widgetSize.width / widgetSize.height.toFloat(),
                    )
                }
            }
        }

        PhotoWidgetAspectRatio.ORIGINAL -> {
            Rect(0, 0, width, height)
        }
    }.also {
        Timber.d("Output rect: $it")
    }
}

private fun tallRect(
    width: Int,
    height: Int,
    scale: Float = PhotoWidgetAspectRatio.TALL.scale,
): Rect {
    val baseWidth = height * scale

    val scaledWidth = baseWidth.roundToInt().coerceAtMost(width)
    val scaledHeight = if (baseWidth > width) {
        ((width / baseWidth) * height).roundToInt()
    } else {
        height
    }

    val top = (height - scaledHeight) / 2
    val left = (width - scaledWidth) / 2

    return Rect(left, top, left + scaledWidth, top + scaledHeight)
}

private fun wideRect(
    width: Int,
    height: Int,
    scale: Float = PhotoWidgetAspectRatio.WIDE.scale,
): Rect {
    val baseHeight = width * scale

    val scaledHeight = baseHeight.roundToInt().coerceAtMost(height)
    val scaledWidth = if (baseHeight > height) {
        ((height / baseHeight) * width).roundToInt()
    } else {
        width
    }

    val top = (height - scaledHeight) / 2
    val left = (width - scaledWidth) / 2

    return Rect(left, top, left + scaledWidth, top + scaledHeight)
}
