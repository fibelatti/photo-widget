package com.fibelatti.photowidget.model

import android.graphics.Matrix
import android.graphics.RectF
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import kotlin.math.min

object PhotoWidgetShapeBuilder {

    private val shapes: List<PhotoWidgetShape> = listOf(
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

    fun defaultShapeId(): String = shapes.first().id

    fun buildAllShapes(
        width: Int,
        height: Int,
        bounds: RectF? = null,
    ): Map<PhotoWidgetShape, RoundedPolygon> = shapes.associateWith { shape ->
        buildShape(photoWidgetShape = shape).also {
            it.transform(
                matrix = calculateMatrix(
                    bounds = bounds ?: it.bounds,
                    width = width,
                    height = height,
                ),
            )
        }
    }

    fun buildShape(
        shapeId: String?,
        width: Int,
        height: Int,
        bounds: RectF? = null,
    ): RoundedPolygon = buildShape(
        photoWidgetShape = shapes.firstOrNull { it.id == shapeId } ?: shapes.first(),
    ).also {
        it.transform(
            matrix = calculateMatrix(
                bounds = bounds ?: it.bounds,
                width = width,
                height = height,
            ),
        )
    }

    fun resizeShape(
        roundedPolygon: RoundedPolygon,
        width: Int,
        height: Int,
        bounds: RectF = RectF(0f, 0f, 1f, 1f),
    ): RoundedPolygon = RoundedPolygon(source = roundedPolygon).also {
        it.transform(
            matrix = calculateMatrix(
                bounds = bounds,
                width = width,
                height = height,
            ),
        )
    }

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

        return polygon.also {
            it.transform(
                Matrix().apply {
                    postRotate(photoWidgetShape.rotation)
                },
            )
        }
    }

    private fun calculateMatrix(bounds: RectF, width: Int, height: Int): Matrix {
        val scale = calculateScale(bounds = bounds, width = width, height = height)
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

    private fun calculateScale(bounds: RectF, width: Int, height: Int): Float {
        val scaleX = width / (bounds.right - bounds.left)
        val scaleY = height / (bounds.bottom - bounds.top)

        return min(scaleX, scaleY)
    }
}
