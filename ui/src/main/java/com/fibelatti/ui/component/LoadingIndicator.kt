@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.util.fastForEach
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A contained, indeterminate loading indicator that morphs between [LoadingIndicator.Polygons].
 *
 * Mirrors the indeterminate `ContainedLoadingIndicator` from Material 3, which builds its [Morph]
 * sequence per instance on first composition. Building a [Morph] is expensive, so with several
 * instances on screen that work causes jank. This implementation builds the sequence once and
 * shares it across all instances.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
) {
    val containerColor = LoadingIndicatorDefaults.containedContainerColor
    val indicatorColor = LoadingIndicatorDefaults.containedIndicatorColor
    val containerShape = LoadingIndicatorDefaults.containerShape

    val morphProgress = remember { Animatable(0f) }
    var morphRotationTargetAngle by remember { mutableFloatStateOf(QUARTER_ROTATION) }
    val globalRotation = remember { Animatable(0f) }
    var currentMorphIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val morphAnimationBlock = {
            launch {
                val morphAnimationSpec = spring(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
                while (true) {
                    val deferred = async {
                        val animationResult = morphProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = morphAnimationSpec,
                        )
                        if (animationResult.endReason == AnimationEndReason.Finished) {
                            currentMorphIndex = (currentMorphIndex + 1) % MorphSequence.size
                            morphProgress.snapTo(0f)
                            morphRotationTargetAngle = (morphRotationTargetAngle + QUARTER_ROTATION) % FULL_ROTATION
                        }
                    }
                    delay(timeMillis = MORPH_INTERVAL_MILLIS)
                    deferred.await()
                }
            }
        }

        val rotationAnimationBlock = {
            launch {
                globalRotation.animateTo(
                    targetValue = FULL_ROTATION,
                    animationSpec = infiniteRepeatable(
                        animation = tween(GLOBAL_ROTATION_DURATION_MILLIS, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                )
            }
        }

        when (val policy = coroutineContext[InfiniteAnimationPolicy]) {
            null -> {
                morphAnimationBlock()
                rotationAnimationBlock()
            }

            else -> policy.onInfiniteOperation {
                morphAnimationBlock()
                rotationAnimationBlock()
            }
        }
    }

    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }

    Box(
        modifier = modifier
            .progressSemantics()
            .size(
                width = LoadingIndicatorDefaults.ContainerWidth,
                height = LoadingIndicatorDefaults.ContainerHeight,
            )
            .clip(containerShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            modifier = Modifier
                .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = true)
                .drawWithContent {
                    val progress = morphProgress.value
                    rotate(degrees = progress * 90 + morphRotationTargetAngle + globalRotation.value) {
                        MorphSequence[currentMorphIndex].toPath(progress = progress, path = path)

                        scaleMatrix.reset()
                        scaleMatrix.scale(x = size.width * ScaleFactor, y = size.height * ScaleFactor)
                        path.transform(scaleMatrix)
                        path.translate(size.center - path.getBounds().center)

                        drawPath(path = path, color = indicatorColor, style = Fill)
                    }
                },
        )
    }
}

object LoadingIndicator {

    val Polygons: List<RoundedPolygon> = listOf(
        MaterialShapes.SoftBurst,
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Pentagon,
        MaterialShapes.Pill,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Oval,
        MaterialShapes.Gem,
        MaterialShapes.Clover8Leaf,
        MaterialShapes.Puffy,
        MaterialShapes.Diamond,
        MaterialShapes.SoftBoom,
        MaterialShapes.Flower,
    )
}

private val MorphSequence: List<Morph> by lazy {
    val normalized = LoadingIndicator.Polygons.map { it.normalized() }
    normalized.indices.map { index ->
        Morph(start = normalized[index], end = normalized[(index + 1) % normalized.size])
    }
}

/**
 * Scales the shapes so they render within the container without clipping as they rotate, at the
 * size defined by the Material 3 spec.
 */
private val ScaleFactor: Float by lazy {
    val bounds = FloatArray(size = 4)
    val maxBounds = FloatArray(size = 4)
    var scaleFactor = 1f

    LoadingIndicator.Polygons.fastForEach { polygon ->
        polygon.calculateBounds(bounds)
        polygon.calculateMaxBounds(maxBounds)
        val scaleX = (bounds[2] - bounds[0]) / (maxBounds[2] - maxBounds[0])
        val scaleY = (bounds[3] - bounds[1]) / (maxBounds[3] - maxBounds[1])
        scaleFactor = min(scaleFactor, max(scaleX, scaleY))
    }

    val activeIndicatorScale = LoadingIndicatorDefaults.IndicatorSize.value /
        min(LoadingIndicatorDefaults.ContainerWidth.value, LoadingIndicatorDefaults.ContainerHeight.value)

    scaleFactor * activeIndicatorScale
}

private fun Morph.toPath(progress: Float, path: Path) {
    val cubics = asCubics(progress)
    path.rewind()
    cubics.firstOrNull()?.let { path.moveTo(it.anchor0X, it.anchor0Y) }
    cubics.fastForEach { cubic ->
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y,
        )
    }
    path.close()
}

private const val GLOBAL_ROTATION_DURATION_MILLIS = 4666
private const val MORPH_INTERVAL_MILLIS = 650L
private const val FULL_ROTATION = 360f
private const val QUARTER_ROTATION = FULL_ROTATION / 4f

@Composable
@PreviewAll
private fun LoadingIndicatorPreview() {
    ExtendedTheme {
        LoadingIndicator()
    }
}
