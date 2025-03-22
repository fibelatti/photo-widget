@file:Suppress("unused")

package com.fibelatti.ui.foundation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

inline fun Modifier.conditional(
    predicate: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier = { this },
): Modifier = if (predicate) then(ifTrue(Modifier)) else then(ifFalse(Modifier))

fun Modifier.fadingEdges(
    scrollState: ScrollState,
    topEdgeHeight: Dp = 72.dp,
    bottomEdgeHeight: Dp = 72.dp,
): Modifier {
    return this then Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()

            val currentScrollPosition = scrollState.value.toFloat()
            val topGradientHeight = min(topEdgeHeight.toPx(), currentScrollPosition)

            if (topGradientHeight > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = currentScrollPosition,
                        endY = currentScrollPosition + topGradientHeight,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }

            val bottomEndY = size.height - scrollState.maxValue + currentScrollPosition
            val remainingScrollDistance = scrollState.maxValue - currentScrollPosition
            val bottomGradientHeight = min(bottomEdgeHeight.toPx(), remainingScrollDistance)

            if (bottomGradientHeight > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = bottomEndY - bottomGradientHeight,
                        endY = bottomEndY,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}

fun Modifier.fadingEdges(
    scrollState: ScrollableState,
    startEdgeSize: Dp = 64.dp,
    endEdgeSize: Dp = 64.dp,
    isHorizontal: Boolean = false,
): Modifier {
    return this then Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()

            if (scrollState.canScrollBackward) {
                drawRect(
                    brush = if (isHorizontal) {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startX = 0f,
                            endX = startEdgeSize.toPx(),
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = startEdgeSize.toPx(),
                        )
                    },
                    blendMode = BlendMode.DstIn,
                )
            }

            if (scrollState.canScrollForward) {
                drawRect(
                    brush = if (isHorizontal) {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startX = size.width - endEdgeSize.toPx(),
                            endX = size.width,
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - endEdgeSize.toPx(),
                            endY = size.height,
                        )
                    },
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}
