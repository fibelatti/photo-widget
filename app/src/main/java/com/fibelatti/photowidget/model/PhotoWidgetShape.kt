package com.fibelatti.photowidget.model

import androidx.annotation.IntRange

sealed class PhotoWidgetShape {

    abstract val id: String
    abstract val rotation: Float
    abstract val scaleX: Float
    abstract val scaleY: Float

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

    data class Custom(
        override val id: String,
        val vertices: FloatArray,
        val rounding: Float = 0f,
        val perVertexRoundness: List<Float>? = null,
        val perVertexSmoothing: List<Float>? = null,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
    ) : PhotoWidgetShape() {

        init {
            require(perVertexRoundness == null || perVertexRoundness.size == vertices.size / 2)
            require(perVertexSmoothing == null || perVertexSmoothing.size == perVertexRoundness?.size)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Custom

            if (id != other.id) return false
            if (!vertices.contentEquals(other.vertices)) return false
            if (rounding != other.rounding) return false
            if (perVertexRoundness != other.perVertexRoundness) return false
            if (perVertexSmoothing != other.perVertexSmoothing) return false
            if (rotation != other.rotation) return false
            if (scaleX != other.scaleX) return false
            if (scaleY != other.scaleY) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + vertices.contentHashCode()
            result = 31 * result + rounding.hashCode()
            result = 31 * result + (perVertexRoundness?.hashCode() ?: 0)
            result = 31 * result + (perVertexSmoothing?.hashCode() ?: 0)
            result = 31 * result + rotation.hashCode()
            result = 31 * result + scaleX.hashCode()
            result = 31 * result + scaleY.hashCode()
            return result
        }

    }
}
