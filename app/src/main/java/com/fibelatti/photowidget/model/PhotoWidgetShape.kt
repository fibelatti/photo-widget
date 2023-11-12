package com.fibelatti.photowidget.model

import androidx.annotation.IntRange

data class PhotoWidgetShape(
    val id: String,
    val type: Type,
    @IntRange(from = 3)
    val numVertices: Int,
    val roundness: Float,
    val innerRadius: Float = .5f,
    val rotation: Float = 0f,
) {

    enum class Type {
        POLYGON,
        STAR,
    }
}
