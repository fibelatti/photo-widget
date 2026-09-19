package com.fibelatti.photowidget.model

/**
 * The rotation that can be applied to shapes flagged with [PhotoWidgetShape.canRotate].
 */
object PhotoWidgetShapeRotation {

    const val DEFAULT: Int = 0

    val values: List<Int> = listOf(DEFAULT, 90, 180, 270)

    fun sanitize(rotation: Int): Int = rotation.takeIf { it in values } ?: DEFAULT

    fun degreesLabel(rotation: Int): String = "$rotation°"
}
