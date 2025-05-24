package com.fibelatti.ui.imageviewer

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

@Composable
fun rememberZoomableImageViewerState(
    minimumScale: Float = 1f,
    maximumScale: Float = 3f,
    onTap: (() -> Unit)? = null,
    onDragToDismiss: (() -> Unit)? = null,
): ZoomableImageViewerState {
    val tap by rememberUpdatedState(onTap)
    val dragToDismiss by rememberUpdatedState(onDragToDismiss)

    return rememberSaveable(saver = ZoomableImageViewerState.Saver) {
        ZoomableImageViewerState(
            minimumScale = minimumScale,
            maximumScale = maximumScale,
        )
    }.apply {
        this.onTap = tap
        this.onDragToDismiss = dragToDismiss
    }
}

@Stable
class ZoomableImageViewerState(
    private val minimumScale: Float = 1f,
    private val maximumScale: Float = 3f,
) {

    var onTap: (() -> Unit)? = null
    var onDragToDismiss: (() -> Unit)? = null

    private var _currentWidthPixel: MutableFloatState = mutableFloatStateOf(0F)
    private var _currentHeightPixel: MutableFloatState = mutableFloatStateOf(0F)
    private var _currentOffsetXPixel: MutableFloatState = mutableFloatStateOf(0F)
    private var _currentOffsetYPixel: MutableFloatState = mutableFloatStateOf(0F)

    val currentWidthPixel: Float by _currentWidthPixel
    val currentHeightPixel: Float by _currentHeightPixel
    val currentOffsetXPixel: Float by _currentOffsetXPixel
    val currentOffsetYPixel: Float by _currentOffsetYPixel

    private var imageAspectRatio: Float = 1F

    private var layoutSize: Size = Size.Zero
    private val standardWidth: Float get() = layoutSize.width
    private val standardHeight: Float get() = standardWidth / imageAspectRatio

    internal val exceed: Boolean get() = !_currentWidthPixel.floatValue.equalsExactly(layoutSize.width)

    internal val isBigVerticalImage: Boolean
        get() {
            if (layoutSize == Size.Zero) return false
            return imageAspectRatio <= layoutSize.aspectRatio
        }

    private var flingAnimation: AnimationScope<Offset, AnimationVector2D>? = null
    private var scaleAnimation: AnimationScope<Float, AnimationVector1D>? = null
    private var resumeOffsetYAnimation: AnimationScope<Float, AnimationVector1D>? = null

    private val draggableBounds: Bounds
        get() {
            return calculateDragBounds(
                imageWidth = _currentWidthPixel.floatValue,
                imageHeight = _currentHeightPixel.floatValue,
            )
        }

    internal fun updateLayoutSize(size: Size) {
        layoutSize = size
        onLayoutSizeChanged()
    }

    internal fun setImageAspectRatio(ratio: Float) {
        if (imageAspectRatio.equalsExactly(ratio)) return
        imageAspectRatio = ratio
        onLayoutSizeChanged()
    }

    private fun onLayoutSizeChanged() {
        _currentWidthPixel.floatValue = standardWidth
        _currentHeightPixel.floatValue = standardHeight
        _currentOffsetXPixel.floatValue = 0F
        if (isBigVerticalImage) {
            _currentOffsetYPixel.floatValue = 0F
        } else {
            _currentOffsetYPixel.floatValue = layoutSize.height / 2F - standardHeight / 2F
        }
    }

    internal suspend fun animateToStandard() {
        val layoutSize: Size = layoutSize
        if (layoutSize == Size.Zero) return
        val targetWidth: Float = standardWidth
        val targetHeight: Float = standardHeight
        animateToTarget(
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetOffsetX = 0F,
            targetOffsetY = layoutSize.height / 2F - targetHeight / 2F,
        )
    }

    internal suspend fun animateToBig(point: Offset) {
        val layoutSize: Size = layoutSize
        if (layoutSize == Size.Zero) return

        val targetWidth: Float = standardWidth * maximumScale
        val targetHeight: Float = targetWidth / imageAspectRatio
        var targetOffsetX: Float = currentOffsetXPixel * maximumScale
        var targetOffsetY: Float = layoutSize.height / 2F - targetHeight / 2F

        if (point.isSpecified && point.isValid()) {
            // tap point must be in the image bounds
            if (point.y < currentOffsetYPixel || point.y > (currentOffsetYPixel + currentHeightPixel)) return
            val xRatio: Float = point.x / currentWidthPixel
            val yRatio: Float = (point.y - currentOffsetYPixel) / currentHeightPixel
            targetOffsetX = -(targetWidth * xRatio - point.x)
            targetOffsetY = point.y - targetHeight * yRatio
        }
        val dragBounds: Bounds = calculateDragBounds(
            imageWidth = targetWidth,
            imageHeight = targetHeight,
        )
        animateToTarget(
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetOffsetX = dragBounds.coerceInX(targetOffsetX),
            targetOffsetY = dragBounds.coerceInY(targetOffsetY),
        )
    }

    internal fun drag(dragAmount: Offset) {
        cancelAnimation()
        if (exceed || isBigVerticalImage) {
            dragForVisit(dragAmount)
        } else {
            dragForExit(dragAmount)
        }
    }

    private fun dragForVisit(dragAmount: Offset) {
        val currentOffset = Offset(x = _currentOffsetXPixel.floatValue, y = _currentOffsetYPixel.floatValue)
        val newOffset: Offset = currentOffset + dragAmount
        val fixedOffset: Offset = draggableBounds.coerceIn(newOffset)
        _currentOffsetXPixel.floatValue = fixedOffset.x
        _currentOffsetYPixel.floatValue = fixedOffset.y
    }

    private fun dragForExit(dragAmount: Offset) {
        val dragAmountY: Float = dragAmount.y
        if (dragAmountY <= 0F) {
            if (_currentOffsetYPixel.floatValue > 0F) {
                _currentOffsetYPixel.floatValue += dragAmountY
            }
        } else {
            _currentOffsetYPixel.floatValue += dragAmountY
        }
    }

    internal suspend fun dragStop(initialVelocity: Velocity) {
        cancelAnimation()
        if (!exceed && !isBigVerticalImage) {
            dragStopForExit()
            return
        }
        val initialValue = Offset(x = _currentOffsetXPixel.floatValue, y = _currentOffsetYPixel.floatValue)
        AnimationState(
            typeConverter = Offset.VectorConverter,
            initialValue = initialValue,
            initialVelocity = initialVelocity.toOffset(),
        ).animateDecay(exponentialDecay()) {
            flingAnimation = this
            if (draggableBounds.outsideAbsolute(value) ||
                velocity.getDistance() <= 300
            ) {
                flingAnimation = null
                cancelAnimation()
                return@animateDecay
            }
            val progressOffset = draggableBounds.coerceIn(value)
            _currentOffsetXPixel.floatValue = progressOffset.x
            _currentOffsetYPixel.floatValue = progressOffset.y
        }
    }

    private suspend fun dragStopForExit() {
        cancelAnimation()
        val standardOffsetY: Float = layoutSize.height / 2F - _currentHeightPixel.floatValue / 2F
        val totalAmount: Float = _currentOffsetYPixel.floatValue - standardOffsetY
        val exitOffsetYThresholds: Float = standardHeight * 0.3F
        if (onDragToDismiss != null && totalAmount > exitOffsetYThresholds) {
            onDragToDismiss?.invoke()
        } else {
            val anim = AnimationState(initialValue = _currentOffsetYPixel.floatValue)
            anim.animateTo(
                targetValue = standardOffsetY,
                animationSpec = tween(durationMillis = ANIMATION_DURATION),
            ) {
                resumeOffsetYAnimation = this
                _currentOffsetYPixel.floatValue = value
            }
        }
    }

    private suspend fun animateToTarget(
        targetWidth: Float,
        targetHeight: Float,
        targetOffsetX: Float,
        targetOffsetY: Float,
    ) {
        cancelAnimation()
        val startWidth: Float = currentWidthPixel
        val startHeight: Float = currentHeightPixel
        val startOffsetX: Float = currentOffsetXPixel
        val startOffsetY: Float = currentOffsetYPixel
        if (startWidth != targetWidth ||
            startHeight != targetHeight ||
            startOffsetX != targetOffsetX ||
            startOffsetY != targetOffsetY
        ) {
            val widthDiff: Float = targetWidth - startWidth
            val heightDiff: Float = targetHeight - startHeight
            val offsetXDiff: Float = targetOffsetX - startOffsetX
            val offsetYDiff: Float = targetOffsetY - startOffsetY
            val anim = AnimationState(initialValue = 0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = ANIMATION_DURATION),
            ) {
                scaleAnimation = this
                val progress: Float = value
                if (widthDiff != 0F) {
                    _currentWidthPixel.floatValue = startWidth + widthDiff * progress
                }
                if (heightDiff != 0F) {
                    _currentHeightPixel.floatValue = startHeight + heightDiff * progress
                }
                if (offsetXDiff != 0F) {
                    _currentOffsetXPixel.floatValue = startOffsetX + offsetXDiff * progress
                }
                if (offsetYDiff != 0F) {
                    _currentOffsetYPixel.floatValue = startOffsetY + offsetYDiff * progress
                }
            }
        }
    }

    private fun cancelAnimation() {
        scaleAnimation?.takeIf { it.isRunning }?.cancelAnimation()
        scaleAnimation = null
        flingAnimation?.takeIf { it.isRunning }?.cancelAnimation()
        flingAnimation = null
        resumeOffsetYAnimation?.takeIf { it.isRunning }?.cancelAnimation()
        resumeOffsetYAnimation = null
    }

    internal fun zoom(centroid: Offset, zoom: Float) {
        val newWidth: Float = (currentWidthPixel * zoom).coerceInWidth()
        val newHeight: Float = (currentHeightPixel * zoom).coerceInHeight()
        val xRatio: Float = (centroid.x - currentOffsetXPixel) / currentWidthPixel
        val yRatio: Float = (centroid.y - currentOffsetYPixel) / currentHeightPixel
        _currentWidthPixel.floatValue = newWidth
        _currentHeightPixel.floatValue = newHeight
        val xOffset: Float = -(newWidth * xRatio - centroid.x)
        val yOffset: Float = -(newHeight * yRatio - centroid.y)
        val bounds: Bounds = calculateDragBounds(imageWidth = newWidth, imageHeight = newHeight)
        _currentOffsetYPixel.floatValue = bounds.coerceInY(yOffset)
        if (newWidth == standardWidth) {
            _currentOffsetXPixel.floatValue = 0F
        } else {
            _currentOffsetXPixel.floatValue = bounds.coerceInX(xOffset)
        }
    }

    private fun calculateDragBounds(imageWidth: Float, imageHeight: Float): Bounds {
        val left: Float
        val right: Float
        if (imageWidth > layoutSize.width) {
            left = -(imageWidth - layoutSize.width)
            right = 0F
        } else {
            left = (layoutSize.width - imageWidth) / 2F
            right = left
        }
        val top: Float
        val bottom: Float
        if (imageHeight > layoutSize.height) {
            top = -(imageHeight - layoutSize.height)
            bottom = 0F
        } else {
            top = (layoutSize.height - imageHeight) / 2F
            bottom = top
        }
        return Bounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    private fun Float.coerceInWidth(): Float {
        return coerceIn(minimumValue = standardWidth, maximumValue = standardWidth * maximumScale)
    }

    private fun Float.coerceInHeight(): Float {
        return coerceIn(minimumValue = standardHeight, maximumValue = standardHeight * maximumScale)
    }

    private fun Float.equalsExactly(target: Float): Boolean = abs(target - this) <= 0.000001F

    private fun Velocity.toOffset(): Offset = Offset(x = x, y = y)

    private val Size.aspectRatio: Float
        get() = width / height

    internal companion object Companion {

        private const val ANIMATION_DURATION = 200

        val Saver: Saver<ZoomableImageViewerState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.minimumScale,
                    it.maximumScale,
                )
            },
            restore = {
                ZoomableImageViewerState(
                    minimumScale = it[0] as Float,
                    maximumScale = it[1] as Float,
                )
            },
        )
    }
}

private data class Bounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {

    val isEmpty: Boolean = (right - left) * (bottom - top) == 0F

    fun inside(x: Float, y: Float): Boolean = xInside(x) && yInside(y)

    fun outside(x: Float, y: Float): Boolean = !inside(x = x, y = y)

    fun inside(offset: Offset): Boolean = inside(x = offset.x, y = offset.y)

    fun outside(offset: Offset): Boolean = outside(x = offset.x, y = offset.y)

    fun xInside(x: Float): Boolean = x >= left && x <= right

    fun yInside(y: Float): Boolean = y >= top && y <= bottom

    fun xOutside(x: Float): Boolean = !xInside(x)

    fun yOutside(y: Float): Boolean = !yInside(y)

    fun outsideAbsolute(offset: Offset): Boolean = xOutside(offset.x) && yOutside(offset.y)

    fun coerceInY(y: Float): Float = y.coerceIn(minimumValue = top, maximumValue = bottom)

    fun coerceInX(x: Float): Float = x.coerceIn(minimumValue = left, maximumValue = right)

    fun coerceIn(offset: Offset): Offset = Offset(x = coerceInX(offset.x), y = coerceInY(offset.y))
}
