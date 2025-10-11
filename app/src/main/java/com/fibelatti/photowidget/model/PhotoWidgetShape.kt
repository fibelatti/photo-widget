package com.fibelatti.photowidget.model

import androidx.annotation.IntRange
import androidx.graphics.shapes.RoundedPolygon

sealed class PhotoWidgetShape {

    abstract val id: String
    abstract val rotation: Float
    abstract val enabled: Boolean

    data class CustomPath(
        override val id: String,
    ) : PhotoWidgetShape() {

        override val rotation: Float = Float.NaN
        override val enabled: Boolean = true
    }

    data class Polygon(
        override val id: String,
        @IntRange(from = 3)
        val numVertices: Int,
        val rounding: Float,
        override val rotation: Float = 0f,
    ) : PhotoWidgetShape() {

        override val enabled: Boolean = true
    }

    data class Star(
        override val id: String,
        @IntRange(from = 3)
        val numVertices: Int,
        val rounding: Float,
        val innerRadius: Float,
        val innerRounding: Float? = null,
        override val rotation: Float = 0f,
    ) : PhotoWidgetShape() {

        override val enabled: Boolean = true
    }

    data class Material(
        override val id: String,
        val roundedPolygon: RoundedPolygon,
        override val enabled: Boolean = true,
    ) : PhotoWidgetShape() {

        override val rotation: Float = Float.NaN
    }
}
