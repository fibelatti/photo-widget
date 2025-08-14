/**
 * See the original implementation at https://github.com/0xZhangKe/ImageViewer/.
 */
package com.fibelatti.ui.imageviewer

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import com.fibelatti.ui.foundation.detectZoom
import com.fibelatti.ui.foundation.pxToDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val infinityConstraints = Constraints()

@Composable
fun ZoomableImageViewer(
    modifier: Modifier = Modifier,
    state: ZoomableImageViewerState = rememberZoomableImageViewerState(),
    content: @Composable () -> Unit,
) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    var latestSize: Size? by remember { mutableStateOf(null) }

    Layout(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val currentSize = Size(
                    width = coordinates.size.width.toFloat(),
                    height = coordinates.size.height.toFloat(),
                )
                if (currentSize != latestSize) {
                    state.updateLayoutSize(currentSize)
                    latestSize = currentSize
                }
            }
            .pointerInput(state) {
                detectTapGestures(
                    onDoubleTap = {
                        if (state.exceed) {
                            coroutineScope.launch {
                                state.animateToStandard()
                            }
                        } else {
                            coroutineScope.launch {
                                state.animateToBig(it)
                            }
                        }
                    },
                    onTap = {
                        state.onTap?.invoke()
                    },
                )
            }
            .draggableInfinity(
                exceed = state.exceed,
                isBigVerticalImage = state.isBigVerticalImage,
                onDrag = state::drag,
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        state.dragStop(velocity)
                    }
                },
            )
            .pointerInput(state) {
                detectZoom(state::zoom)
            },
        content = {
            Layout(
                modifier = Modifier
                    .offset(
                        x = state.currentOffsetXPixel.pxToDp(),
                        y = state.currentOffsetYPixel.pxToDp(),
                    )
                    .width(state.currentWidthPixel.pxToDp())
                    .height(state.currentHeightPixel.pxToDp()),
                content = content,
            ) { measurables, constraints ->
                if (measurables.size > 1) {
                    error("ZoomableImageViewer must only have one child!")
                }
                val firstMeasurable: Measurable = measurables.first()
                val placeable: Placeable = firstMeasurable.measure(constraints)
                val minWidth: Int = firstMeasurable.minIntrinsicWidth(100)
                val minHeight: Int = firstMeasurable.minIntrinsicHeight(100)
                if (minWidth > 0 && minHeight > 0) {
                    state.setImageAspectRatio(minWidth / minHeight.toFloat())
                }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(infinityConstraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(0, 0)
        }
    }
}

private fun Modifier.draggableInfinity(
    exceed: Boolean,
    isBigVerticalImage: Boolean,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragStopped: (velocity: Velocity) -> Unit,
): Modifier {
    val velocityTracker = VelocityTracker()
    return Modifier.pointerInput(exceed || isBigVerticalImage) {
        if (exceed) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    velocityTracker.addPointerInputChange(change)
                    onDrag(dragAmount)
                },
                onDragEnd = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
                onDragCancel = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
            )
        } else {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    velocityTracker.addPointerInputChange(change)
                    onDrag(Offset(x = 0F, y = dragAmount))
                },
                onDragEnd = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
                onDragCancel = {
                    val velocity = velocityTracker.calculateVelocity()
                    onDragStopped(velocity)
                },
            )
        }
    } then this
}
