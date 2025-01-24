package com.fibelatti.photowidget.model

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object PhotoWidgetShapeBuilder {

    val shapes: List<PhotoWidgetShape> = listOf(
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
            innerRounding = .32f,
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
        PhotoWidgetShape.Simple(id = "heart"),
        PhotoWidgetShape.Simple(id = "star"),
    )

    fun getShapePath(
        shapeId: String,
        width: Float,
        height: Float,
        rectF: RectF = RectF(0f, 0f, width, height),
    ): Path {
        val photoWidgetShape: PhotoWidgetShape = shapes.firstOrNull { it.id == shapeId } ?: shapes.first()

        val polygon: RoundedPolygon = when (photoWidgetShape) {
            is PhotoWidgetShape.Simple -> {
                return when (shapeId) {
                    "heart" -> {
                        createHeartPath(width = rectF.width(), height = rectF.height())
                    }

                    "star" -> {
                        createStarPath(width = rectF.width(), height = rectF.height())
                    }

                    else -> error("Unknown simple shapeId: $shapeId")
                }
            }

            is PhotoWidgetShape.Polygon -> {
                RoundedPolygon(
                    numVertices = photoWidgetShape.numVertices,
                    rounding = CornerRounding(radius = photoWidgetShape.rounding),
                )
            }

            is PhotoWidgetShape.Star -> {
                RoundedPolygon.star(
                    numVerticesPerRadius = photoWidgetShape.numVertices,
                    innerRadius = photoWidgetShape.innerRadius,
                    rounding = CornerRounding(radius = photoWidgetShape.rounding),
                    innerRounding = photoWidgetShape.innerRounding?.let(::CornerRounding),
                )
            }
        }

        return polygon.transformed(
            matrix = Matrix().apply {
                postRotate(photoWidgetShape.rotation)
                postScale(photoWidgetShape.scaleX, photoWidgetShape.scaleY)
            },
        ).transformed(
            width = width,
            height = height,
        ).transformed(
            bounds = rectF,
        ).toPath()
    }

    private fun RoundedPolygon.transformed(
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

    private fun createHeartPath(width: Float, height: Float): Path {
        return Path().apply {
            // Starting point
            moveTo(width / 2, height / 5)

            // Upper left path
            cubicTo(
                5 * width / 14,
                0f,
                0f,
                height / 15,
                width / 28,
                2 * height / 5,
            )

            // Lower left path
            cubicTo(
                width / 14,
                2 * height / 3,
                3 * width / 7,
                5 * height / 6,
                width / 2,
                height,
            )

            // Lower right path
            cubicTo(
                4 * width / 7,
                5 * height / 6,
                13 * width / 14,
                2 * height / 3,
                27 * width / 28,
                2 * height / 5,
            )

            // Upper right path
            cubicTo(
                width,
                height / 15,
                9 * width / 14,
                0f,
                width / 2,
                height / 5,
            )

            close()
        }
    }

    private fun createStarPath(width: Float, height: Float, points: Int = 5): Path {
        val midWidth = width / 2
        val midHeight = height / 2
        val outerRadius = min(width, height) / 2
        val innerRadius = outerRadius / 1.5

        val angleStep = 2 * Math.PI / points
        val startAngle = -Math.PI / 2 // Start at the top of the star

        return Path().apply {
            for (i in 0 until points) {
                val outerAngle = startAngle + i * angleStep
                val innerAngle = outerAngle + angleStep / 2

                val outerX = midWidth + (outerRadius * cos(outerAngle)).toFloat()
                val outerY = midHeight + (outerRadius * sin(outerAngle)).toFloat()
                val innerX = midWidth + (innerRadius * cos(innerAngle)).toFloat()
                val innerY = midHeight + (innerRadius * sin(innerAngle)).toFloat()

                if (i == 0) {
                    moveTo(outerX, outerY)
                } else {
                    lineTo(outerX, outerY)
                }
                lineTo(innerX, innerY)
            }

            close()
        }
    }
}
