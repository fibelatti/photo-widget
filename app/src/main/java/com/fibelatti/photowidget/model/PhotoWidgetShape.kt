package com.fibelatti.photowidget.model

import androidx.annotation.IntRange

sealed class PhotoWidgetShape {

    abstract val id: String
    abstract val rotation: Float
    abstract val scaleX: Float
    abstract val scaleY: Float

    data class Simple(override val id: String) : PhotoWidgetShape() {

        override val rotation: Float = 0f
        override val scaleX: Float = 1f
        override val scaleY: Float = 1f
    }

    data class Polygon(
        override val id: String,
        @IntRange(from = 3)
        val numVertices: Int,
        val rounding: Float,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
    ) : PhotoWidgetShape()

    data class Star(
        override val id: String,
        @IntRange(from = 3)
        val numVertices: Int,
        val rounding: Float,
        val innerRadius: Float,
        val innerRounding: Float? = null,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
    ) : PhotoWidgetShape()
}
