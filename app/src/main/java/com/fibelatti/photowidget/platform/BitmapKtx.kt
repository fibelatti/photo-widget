package com.fibelatti.photowidget.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.toRectF
import androidx.graphics.shapes.toPath
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.transformed
import kotlin.math.min
import kotlin.math.roundToInt

fun Bitmap.withRoundedCorners(
    desiredAspectRatio: PhotoWidgetAspectRatio,
    radius: Float = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
): Bitmap = withTransformation(desiredAspectRatio = desiredAspectRatio) { canvas, rect, paint ->
    canvas.drawRoundRect(rect.toRectF(), radius, radius, paint)
}

fun Bitmap.withPolygonalShape(
    shapeId: String,
): Bitmap = withTransformation(desiredAspectRatio = PhotoWidgetAspectRatio.SQUARE) { canvas, rect, paint ->
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
    desiredAspectRatio: PhotoWidgetAspectRatio,
    body: (Canvas, Rect, Paint) -> Unit,
): Bitmap {
    val source = when (desiredAspectRatio) {
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

        PhotoWidgetAspectRatio.ORIGINAL -> Rect(0, 0, width, height)
    }
    val destination = Rect(0, 0, source.width(), source.height())

    val output = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply {
        isAntiAlias = true
    }

    canvas.drawARGB(0, 0, 0, 0)

    body(canvas, if (PhotoWidgetAspectRatio.SQUARE == desiredAspectRatio) source else destination, paint)

    paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
    canvas.drawBitmap(this, source, destination, paint)

    return output
}
