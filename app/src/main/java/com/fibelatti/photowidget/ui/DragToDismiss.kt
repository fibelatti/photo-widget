package com.fibelatti.photowidget.ui

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity

/**
 * A [Modifier] that detects vertical drag gestures and notifies the caller.
 */
fun Modifier.onVerticalDrag(
    onDrag: (dragAmount: Offset) -> Unit,
    onDragStopped: (velocity: Velocity) -> Unit,
): Modifier {
    val velocityTracker = VelocityTracker()
    return Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)
                onDrag(Offset(x = 0F, y = dragAmount))
            },
            onDragEnd = {
                onDragStopped(velocityTracker.calculateVelocity())
            },
            onDragCancel = {
                onDragStopped(velocityTracker.calculateVelocity())
            },
        )
    } then this
}

@Composable
fun rememberDragToDismissState(
    onDragToDismiss: (() -> Unit),
): DragToDismissState {
    val dragToDismiss: () -> Unit by rememberUpdatedState(onDragToDismiss)

    return remember {
        DragToDismissState()
    }.apply {
        this.onDragToDismiss = dragToDismiss
    }
}

@Stable
class DragToDismissState {

    var currentOffsetYPixel: Float by mutableFloatStateOf(0F)
        private set
    private var currentHeightPixel: Float by mutableFloatStateOf(0F)

    private var resumeOffsetYAnimation: AnimationScope<Float, AnimationVector1D>? = null

    var onDragToDismiss: (() -> Unit)? = null

    fun setReferenceHeight(height: Float) {
        currentHeightPixel = height
    }

    fun onDrag(amount: Offset) {
        cancelAnimation()
        val dragAmountY: Float = amount.y
        if (dragAmountY <= 0F) {
            if (currentOffsetYPixel > 0F) {
                currentOffsetYPixel += dragAmountY
            }
        } else {
            currentOffsetYPixel += dragAmountY
        }
    }

    suspend fun onDragStopped() {
        cancelAnimation()
        val exitOffsetYThreshold: Float = if (currentOffsetYPixel > 0) currentHeightPixel * 0.3F else 500f
        if (onDragToDismiss != null && currentOffsetYPixel > exitOffsetYThreshold) {
            onDragToDismiss?.invoke()
        } else {
            val anim = AnimationState(initialValue = currentOffsetYPixel)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200),
            ) {
                resumeOffsetYAnimation = this
                currentOffsetYPixel = value
            }
        }
    }

    private fun cancelAnimation() {
        resumeOffsetYAnimation?.takeIf { it.isRunning }?.cancelAnimation()
        resumeOffsetYAnimation = null
    }
}
