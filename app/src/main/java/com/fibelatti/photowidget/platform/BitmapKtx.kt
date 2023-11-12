package com.fibelatti.photowidget.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.graphics.toRectF
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.drawPolygon

fun Bitmap.withRoundedCorners(
    radius: Float = 64f,
): Bitmap = withTransformation { canvas, rect, paint ->
    canvas.drawRoundRect(rect.toRectF(), radius, radius, paint)
}

fun Bitmap.withPolygonalShape(
    roundedPolygon: RoundedPolygon,
): Bitmap = withTransformation { canvas, _, paint ->
    canvas.drawPolygon(polygon = roundedPolygon, paint = paint)
}

private inline fun Bitmap.withTransformation(body: (Canvas, Rect, Paint) -> Unit): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply {
        isAntiAlias = true
    }
    val rect = Rect(0, 0, width, height)

    canvas.drawARGB(0, 0, 0, 0)

    body(canvas, rect, paint)

    paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
    canvas.drawBitmap(this, rect, rect, paint)

    return output
}
