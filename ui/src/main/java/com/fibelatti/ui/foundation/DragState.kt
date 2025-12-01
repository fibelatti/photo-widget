package com.fibelatti.ui.foundation

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * A [Modifier] that detects vertical drag gestures and notifies the caller.
 *
 * @param onDrag called for each vertical drag event with the total [amount][Offset].
 * @param onDragStopped called when the gesture ends, or if another gesture has consumed the
 * pointer input, cancelling the drag.
 * @param enabled whether the gesture detector is enabled.
 */
fun Modifier.onVerticalDrag(
    onDrag: (amount: Offset) -> Unit,
    onDragStopped: (velocity: Velocity) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val velocityTracker = VelocityTracker()
    return Modifier.pointerInput(onDrag, onDragStopped, enabled) {
        if (!enabled) return@pointerInput

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

/**
 * A [Modifier] that detects horizontal drag gestures and notifies the caller.
 *
 * @param onDrag called for each vertical drag event with the total [amount][Offset].
 * @param onDragStopped called when the gesture ends, or if another gesture has consumed the
 * pointer input, cancelling the drag.
 * @param enabled whether the gesture detector is enabled.
 */
fun Modifier.onHorizontalDrag(
    onDrag: (dragAmount: Offset) -> Unit,
    onDragStopped: (velocity: Velocity) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val velocityTracker = VelocityTracker()
    return Modifier.pointerInput(onDrag, onDragStopped, enabled) {
        if (!enabled) return@pointerInput

        detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
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

/**
 * Creates a [DragState] that can be paired with [Modifier.onVerticalDrag] or
 * [Modifier.onHorizontalDrag], keeping track of the drag amount and notifying the caller when
 * the user confirms their intent via the gesture, and optionally when the confirmation threshold
 * is crossed.
 *
 * @param mode whether the drag gesture is valid in a unidirectional or bidirectional fashion. When
 * using [DragState.Mode.UNIDIRECTIONAL], the gesture is only confirmed when dragging towards the
 * end or the bottom of the screen.
 * @param onConfirm invoked when the user finishes the gesture beyond the threshold, with the
 * [direction][DragState.Direction] of the gesture.
 * @param onThreshold invoked once every time the threshold is crossed.
 */
@Composable
fun rememberDragState(
    mode: DragState.Mode,
    onConfirm: (DragState.Direction) -> Unit,
    onThreshold: (() -> Unit)? = null,
): DragState {
    val confirmState: (DragState.Direction) -> Unit by rememberUpdatedState(onConfirm)
    val thresholdState: (() -> Unit)? by rememberUpdatedState(onThreshold)

    return rememberSaveable(saver = DragState.Saver) {
        DragState(mode = mode)
    }.apply {
        this.onConfirm = confirmState
        this.onThreshold = thresholdState
    }
}

@Stable
class DragState internal constructor(private val mode: Mode) {

    /**
     * The current offset of the drag gesture from where it started, in pixels.
     */
    var currentOffsetPixel: Float by mutableFloatStateOf(0F)
        private set

    /**
     * The current offset of the drag gesture from where it started, as a fraction (0f..1f) of the
     * threshold.
     */
    var currentOffsetFraction: Float by mutableFloatStateOf(0F)
        private set

    private val currentOffsetForMode: Float
        get() = if (mode == Mode.UNIDIRECTIONAL) currentOffsetPixel else abs(currentOffsetPixel)

    private var threshold: Float = 500f
    private var didNotifyThreshold: Boolean = false
    private var resumeOffsetAnimation: AnimationScope<Float, AnimationVector1D>? = null

    internal var onConfirm: ((Direction) -> Unit)? = null
    internal var onThreshold: (() -> Unit)? = null

    /**
     * Respond to a drag gesture event by adding the given [amount] to the current offset.
     *
     * @param amount the amount to add to the current offset.
     */
    fun onDrag(amount: Offset) {
        cancelAnimation()

        currentOffsetPixel += amount.y
        currentOffsetFraction = (abs(currentOffsetPixel) / threshold).coerceIn(0f, 1f)

        if (!didNotifyThreshold && currentOffsetForMode >= threshold) {
            didNotifyThreshold = true
            onThreshold?.invoke()
        } else if (currentOffsetForMode < threshold) {
            didNotifyThreshold = false
        }
    }

    /**
     * Respond to the end of a drag gesture, invoking the confirmation lambda if the accumulated
     * offset is above the threshold.
     *
     * @param resetOnConfirm whether the offset should be reset to 0 when the gesture is confirmed.
     * The offset is always reset when it isn't.
     */
    suspend fun onDragStopped(resetOnConfirm: Boolean = false) {
        cancelAnimation()

        if (currentOffsetForMode >= threshold) {
            onConfirm?.invoke(if (currentOffsetPixel > 0) Direction.END else Direction.START)
        }

        if (currentOffsetForMode < threshold || resetOnConfirm) {
            val anim: AnimationState<Float, AnimationVector1D> = AnimationState(initialValue = currentOffsetPixel)
            anim.animateTo(targetValue = 0f, animationSpec = spring(dampingRatio = .6f)) {
                resumeOffsetAnimation = this
                currentOffsetPixel = value
            }
        }
    }

    /**
     * Defines the gesture threshold to trigger the confirmation lambda.
     *
     * If not set, 500px is used by default.
     *
     * @param value the threshold in pixels.
     */
    fun setThreshold(value: Float) {
        threshold = value
    }

    private fun cancelAnimation() {
        resumeOffsetAnimation?.takeIf { it.isRunning }?.cancelAnimation()
        resumeOffsetAnimation = null
    }

    /**
     * The direction of the drag gesture.
     */
    enum class Direction {

        /**
         * Towards the start (or top) of the screen.
         */
        START,

        /**
         * Towards the end (or bottom) of the screen.
         */
        END,
    }

    /**
     * Which direction the drag gesture is valid in.
     */
    enum class Mode {

        /**
         * The drag gesture is valid in a single direction, towards the end or bottom of the
         * screen.
         */
        UNIDIRECTIONAL,

        /**
         * The drag gesture is valid in both directions.
         */
        BIDIRECTIONAL,
    }

    internal companion object {

        internal val Saver: Saver<DragState, *> = listSaver(
            save = {
                listOf<Any>(it.mode)
            },
            restore = {
                DragState(
                    mode = it[0] as Mode,
                )
            },
        )
    }
}
