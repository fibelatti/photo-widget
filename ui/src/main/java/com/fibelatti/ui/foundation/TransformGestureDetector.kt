package com.fibelatti.ui.foundation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

suspend fun PointerInputScope.detectZoom(
    onGesture: (centroid: Offset, zoom: Float) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pastTouchSlop = false
        val touchSlop: Float = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event: PointerEvent = awaitPointerEvent()
            val canceled: Boolean = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange: Float = event.calculateZoom()

                if (!pastTouchSlop) {
                    zoom *= zoomChange

                    val centroidSize: Float = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion: Float = abs(1 - zoom) * centroidSize

                    if (zoomMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid: Offset = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f) {
                        onGesture(centroid, zoomChange)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
