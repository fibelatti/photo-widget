package com.fibelatti.photowidget.model

import android.graphics.Matrix
import android.graphics.RectF
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.transformed
import kotlin.math.min

object PhotoWidgetShapeBuilder {

    private val shapes: List<PhotoWidgetShape> = listOf(
        PhotoWidgetShape(
            id = "square",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 4,
            roundness = 0.05f,
            rotation = 45f,
        ),
        PhotoWidgetShape(
            id = "rounded-square",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 4,
            roundness = 0.2f,
            rotation = 45f,
        ),
        PhotoWidgetShape(
            id = "squircle",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 4,
            roundness = 0.6f,
            rotation = 45f,
        ),
        PhotoWidgetShape(
            id = "circle",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 8,
            roundness = 1f,
        ),
        PhotoWidgetShape(
            id = "scallop",
            type = PhotoWidgetShape.Type.STAR,
            numVertices = 12,
            roundness = .1f,
            innerRadius = .928f,
        ),
        PhotoWidgetShape(
            id = "medal",
            type = PhotoWidgetShape.Type.STAR,
            numVertices = 8,
            roundness = .16f,
            innerRadius = .784f,
        ),
        PhotoWidgetShape(
            id = "clover",
            type = PhotoWidgetShape.Type.STAR,
            numVertices = 4,
            roundness = .32f,
            innerRadius = .352f,
            rotation = 45f,
        ),
        PhotoWidgetShape(
            id = "octagon",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 8,
            roundness = .16f,
        ),
        PhotoWidgetShape(
            id = "hexagon",
            type = PhotoWidgetShape.Type.POLYGON,
            numVertices = 6,
            roundness = .16f,
        ),
    )

    fun defaultShapeId(): String = "rounded-square"

    fun buildAllShapes(
        bounds: RectF? = null,
        width: Float = 1f,
        height: Float = 1f,
    ): Map<PhotoWidgetShape, RoundedPolygon> = shapes.associateWith { shape ->
        buildShape(photoWidgetShape = shape).transformed(
            bounds = bounds,
            width = width,
            height = height,
        )
    }

    fun buildShape(
        shapeId: String?,
        bounds: RectF? = null,
        width: Float = 1f,
        height: Float = 1f,
    ): RoundedPolygon = buildShape(
        photoWidgetShape = shapes.firstOrNull { it.id == shapeId } ?: shapes.first(),
    ).transformed(
        bounds = bounds,
        width = width,
        height = height,
    )

    fun resizeShape(
        roundedPolygon: RoundedPolygon,
        width: Float,
        height: Float,
        bounds: RectF = RectF(0f, 0f, 1f, 1f),
    ): RoundedPolygon = RoundedPolygon(source = roundedPolygon).transformed(
        bounds = bounds,
        width = width,
        height = height,
    )

    private fun buildShape(photoWidgetShape: PhotoWidgetShape): RoundedPolygon {
        val polygon = when (photoWidgetShape.type) {
            PhotoWidgetShape.Type.POLYGON -> RoundedPolygon(
                numVertices = photoWidgetShape.numVertices,
                rounding = CornerRounding(radius = photoWidgetShape.roundness),
            )

            PhotoWidgetShape.Type.STAR -> RoundedPolygon.star(
                numVerticesPerRadius = photoWidgetShape.numVertices,
                innerRadius = photoWidgetShape.innerRadius,
                rounding = CornerRounding(radius = photoWidgetShape.roundness),
            )
        }

        return polygon.transformed(
            matrix = Matrix().apply {
                postRotate(photoWidgetShape.rotation)
            },
        )
    }
}

fun RoundedPolygon.transformed(
    bounds: RectF? = null,
    width: Float? = null,
    height: Float? = null,
): RoundedPolygon {
    val actualBounds = bounds ?: calculateBounds().let { RectF(it[0], it[1], it[2], it[3]) }

    return transformed(
        matrix = calculateMatrix(
            bounds = actualBounds,
            width = width ?: actualBounds.width(),
            height = height ?: actualBounds.height(),
        ),
    )
}

private fun calculateMatrix(bounds: RectF, width: Float, height: Float): Matrix {
    val scaleX = width / (bounds.right - bounds.left)
    val scaleY = height / (bounds.bottom - bounds.top)
    val scale = min(scaleX, scaleY)
    val scaledLeft = scale * bounds.left
    val scaledTop = scale * bounds.top
    val scaledWidth = (scale * bounds.right) - scaledLeft
    val scaledHeight = (scale * bounds.bottom) - scaledTop
    val newLeft = scaledLeft - (width - scaledWidth) / 2
    val newTop = scaledTop - (height - scaledHeight) / 2

    return Matrix().apply {
        preTranslate(-newLeft, -newTop)
        preScale(scale, scale)
    }
}
