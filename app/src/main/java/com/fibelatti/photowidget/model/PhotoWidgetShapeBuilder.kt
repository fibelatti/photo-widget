@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.model

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
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
        PhotoWidgetShape.Material(
            id = "squircle",
            roundedPolygon = MaterialShapes.Square,
        ),
        PhotoWidgetShape.Material(
            id = "slanted",
            roundedPolygon = MaterialShapes.Slanted,
        ),
        PhotoWidgetShape.Material(
            id = "arch",
            roundedPolygon = MaterialShapes.Arch,
        ),
        PhotoWidgetShape.Material(
            id = "ghostish",
            roundedPolygon = MaterialShapes.Ghostish,
        ),
        PhotoWidgetShape.Material(
            id = "fan",
            roundedPolygon = MaterialShapes.Fan,
        ),
        PhotoWidgetShape.Polygon(
            id = "circle",
            numVertices = 8,
            rounding = 1f,
        ),
        PhotoWidgetShape.Material(
            id = "oval",
            roundedPolygon = MaterialShapes.Oval,
        ),
        PhotoWidgetShape.Material(
            id = "pill",
            roundedPolygon = MaterialShapes.Pill,
        ),
        PhotoWidgetShape.Material(
            id = "bun",
            roundedPolygon = MaterialShapes.Bun,
            enabled = false, // Scaling is broken
        ),
        PhotoWidgetShape.Material(
            id = "diamond",
            roundedPolygon = MaterialShapes.Diamond,
            enabled = false, // Scaling is broken
        ),
        PhotoWidgetShape.Material(
            id = "gem",
            roundedPolygon = MaterialShapes.Gem,
        ),
        PhotoWidgetShape.Material(
            id = "clover-8-leaf",
            roundedPolygon = MaterialShapes.Clover8Leaf,
        ),
        PhotoWidgetShape.Star(
            id = "daisy",
            numVertices = 12,
            rounding = .5f,
            innerRadius = .5f,
            innerRounding = 0f,
        ),
        PhotoWidgetShape.Material(
            id = "soft-boom",
            roundedPolygon = MaterialShapes.SoftBoom,
            enabled = false, // Doesn't show much of the photo
        ),
        PhotoWidgetShape.Material(
            id = "flower",
            roundedPolygon = MaterialShapes.Flower,
        ),
        PhotoWidgetShape.Material(
            id = "clover",
            roundedPolygon = MaterialShapes.Cookie4Sided,
        ),
        PhotoWidgetShape.Material(
            id = "cookie-6-sided",
            roundedPolygon = MaterialShapes.Cookie6Sided,
        ),
        PhotoWidgetShape.Material(
            id = "cookie-7-sided",
            roundedPolygon = MaterialShapes.Cookie7Sided,
        ),
        PhotoWidgetShape.Material(
            id = "cookie-9-sided",
            roundedPolygon = MaterialShapes.Cookie9Sided,
        ),
        PhotoWidgetShape.Material(
            id = "scallop",
            roundedPolygon = MaterialShapes.Cookie12Sided,
        ),
        PhotoWidgetShape.Material(
            id = "medal",
            roundedPolygon = MaterialShapes.Sunny,
        ),
        PhotoWidgetShape.Material(
            id = "soft-burst",
            roundedPolygon = MaterialShapes.SoftBurst,
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
        PhotoWidgetShape.Material(
            id = "clam-shell",
            roundedPolygon = MaterialShapes.ClamShell,
            enabled = false, // Scaling is broken
        ),
        PhotoWidgetShape.CustomPath(id = "heart"),
        PhotoWidgetShape.Material(
            id = "puffy",
            roundedPolygon = MaterialShapes.Puffy,
            enabled = false, // Scaling is broken
        ),
        PhotoWidgetShape.Material(
            id = "puffy-diamond",
            roundedPolygon = MaterialShapes.PuffyDiamond,
        ),
        PhotoWidgetShape.CustomPath(id = "star"),
        PhotoWidgetShape.Material(
            id = "burst",
            roundedPolygon = MaterialShapes.Burst,
        ),
        PhotoWidgetShape.Material(
            id = "pixel-circle",
            roundedPolygon = MaterialShapes.PixelCircle,
        ),
    ).filter { it.enabled }

    init {
        require(shapes.size == shapes.distinctBy { it.id }.size) {
            "Shape IDs must be unique!"
        }
    }

    fun getShapePath(shapeId: String, size: Float): Path {
        val photoWidgetShape: PhotoWidgetShape = shapes.firstOrNull { it.id == shapeId } ?: shapes.first()

        val polygon: RoundedPolygon = when (photoWidgetShape) {
            is PhotoWidgetShape.CustomPath -> {
                // Return early since custom paths don't have to be transformed further
                return when (shapeId) {
                    "heart" -> createHeartPath(width = size, height = size)
                    "star" -> createStarPath(width = size, height = size)
                    else -> error("Unknown `CustomPath` shapeId: $shapeId")
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

            is PhotoWidgetShape.Material -> {
                photoWidgetShape.roundedPolygon
            }
        }

        return polygon
            .transformedRotation(rotation = photoWidgetShape.rotation)
            .transformedSize(size = size)
            .toPath()
    }

    fun RoundedPolygon.transformedRotation(rotation: Float): RoundedPolygon {
        return transformed(
            matrix = Matrix().apply {
                if (!rotation.isNaN()) postRotate(rotation)
            },
        )
    }

    private fun RoundedPolygon.transformedSize(size: Float): RoundedPolygon {
        return transformed(
            matrix = calculateMatrix(
                size = size,
                bounds = calculateBounds().let { RectF(it[0], it[1], it[2], it[3]) },
            ),
        )
    }

    private fun calculateMatrix(size: Float, bounds: RectF): Matrix {
        val scaleX = size / (bounds.right - bounds.left)
        val scaleY = size / (bounds.bottom - bounds.top)
        val scale = min(scaleX, scaleY)
        val scaledLeft = scale * bounds.left
        val scaledTop = scale * bounds.top
        val scaledWidth = (scale * bounds.right) - scaledLeft
        val scaledHeight = (scale * bounds.bottom) - scaledTop
        val newLeft = scaledLeft - (size - scaledWidth) / 2
        val newTop = scaledTop - (size - scaledHeight) / 2

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
