package com.fibelatti.photowidget.model

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.transformed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object PhotoWidgetShapeBuilder {

    private val shapes: List<PhotoWidgetShape> = listOf(
        PhotoWidgetShape.Polygon(
            id = "square",
            numVertices = 4,
            rounding = 0.05f,
            rotation = 45f,
        ),
        PhotoWidgetShape.Polygon(
            id = "rounded-square",
            numVertices = 4,
            rounding = 0.2f,
            rotation = 45f,
        ),
        PhotoWidgetShape.Polygon(
            id = "squircle",
            numVertices = 4,
            rounding = 0.6f,
            rotation = 45f,
        ),
        PhotoWidgetShape.Polygon(
            id = "circle",
            numVertices = 8,
            rounding = 1f,
        ),
        PhotoWidgetShape.Star(
            id = "daisy",
            numVertices = 12,
            rounding = .5f,
            innerRadius = .5f,
            innerRounding = 0f,
        ),
        PhotoWidgetShape.Star(
            id = "scallop",
            numVertices = 12,
            rounding = .1f,
            innerRadius = .928f,
        ),
        PhotoWidgetShape.Star(
            id = "medal",
            numVertices = 8,
            rounding = .16f,
            innerRadius = .784f,
        ),
        PhotoWidgetShape.Star(
            id = "clover",
            numVertices = 4,
            rounding = .32f,
            innerRadius = .352f,
            rotation = 45f,
        ),
        PhotoWidgetShape.Polygon(
            id = "octagon",
            numVertices = 8,
            rounding = .16f,
        ),
        PhotoWidgetShape.Polygon(
            id = "hexagon",
            numVertices = 6,
            rounding = .16f,
        ),
        PhotoWidgetShape.Custom(
            id = "heart",
            vertices = floatArrayOf(
                radialToCartesian(radius = 0.8f, angleRadians = 270f.toRadians()).x,
                radialToCartesian(radius = 0.8f, angleRadians = 270f.toRadians()).y,
                radialToCartesian(radius = 0.8f, angleRadians = 30f.toRadians()).x,
                radialToCartesian(radius = 0.8f, angleRadians = 30f.toRadians()).y,
                radialToCartesian(radius = 0f, angleRadians = 90f.toRadians()).x,
                radialToCartesian(radius = 0f, angleRadians = 90f.toRadians()).y,
                radialToCartesian(radius = 0.8f, angleRadians = 150f.toRadians()).x,
                radialToCartesian(radius = 0.8f, angleRadians = 150f.toRadians()).y,
            ),
            perVertexRoundness = listOf(0f, .2f, 0f, .2f),
            perVertexSmoothing = listOf(0f, 0.65f, 0f, 0.65f),
            rotation = 180f,
            scaleY = 0.8f,
        ),
    )

    fun buildAllShapes(): Map<PhotoWidgetShape, RoundedPolygon> = shapes.associateWith { shape ->
        buildShape(photoWidgetShape = shape).transformed(
            width = 1f,
            height = 1f,
        )
    }

    fun buildShape(
        shapeId: String?,
        width: Float = 1f,
        height: Float = 1f,
    ): RoundedPolygon = buildShape(
        photoWidgetShape = shapes.firstOrNull { it.id == shapeId } ?: shapes.first(),
    ).transformed(
        width = width,
        height = height,
    )

    fun resizeShape(
        roundedPolygon: RoundedPolygon,
        width: Float,
        height: Float,
    ): RoundedPolygon = RoundedPolygon(source = roundedPolygon).transformed(
        width = width,
        height = height,
    )

    private fun buildShape(photoWidgetShape: PhotoWidgetShape): RoundedPolygon {
        val polygon = when (photoWidgetShape) {
            is PhotoWidgetShape.Polygon -> RoundedPolygon(
                numVertices = photoWidgetShape.numVertices,
                rounding = CornerRounding(radius = photoWidgetShape.rounding),
            )

            is PhotoWidgetShape.Star -> RoundedPolygon.star(
                numVerticesPerRadius = photoWidgetShape.numVertices,
                innerRadius = photoWidgetShape.innerRadius,
                rounding = CornerRounding(radius = photoWidgetShape.rounding),
                innerRounding = photoWidgetShape.innerRounding?.let(::CornerRounding),
            )

            is PhotoWidgetShape.Custom -> RoundedPolygon(
                vertices = photoWidgetShape.vertices,
                rounding = CornerRounding(radius = photoWidgetShape.rounding),
                perVertexRounding = photoWidgetShape.perVertexRoundness?.mapIndexed { index, value ->
                    CornerRounding(
                        radius = value,
                        smoothing = photoWidgetShape.perVertexSmoothing?.get(index) ?: 0f,
                    )
                },
            )
        }

        return polygon.transformed(
            matrix = Matrix().apply {
                postRotate(photoWidgetShape.rotation)
                postScale(photoWidgetShape.scaleX, photoWidgetShape.scaleY)
            },
        )
    }

    private fun radialToCartesian(
        radius: Float,
        angleRadians: Float,
        center: PointF = PointF(0f, 0f),
    ): PointF = directionVectorPointF(angleRadians) * radius + center

    private fun directionVectorPointF(
        angleRadians: Float,
    ): PointF = PointF(cos(angleRadians), sin(angleRadians))

    private fun Float.toRadians() = this * PI.toFloat() / 180f
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
