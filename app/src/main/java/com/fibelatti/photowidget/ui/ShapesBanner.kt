package com.fibelatti.photowidget.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.configure.ColoredShape
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun ShapesBanner(
    modifier: Modifier = Modifier,
    polygonSize: Dp = 48.dp,
) {
    val polygons = remember {
        listOf("scallop", "medal", "clover", "octagon", "hexagon")
            .shuffled()
            .map(PhotoWidgetShapeBuilder::buildShape)
    }
    var isAnimating by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "ShapesBannerTransition")
    var desiredEasing by remember { mutableFloatStateOf(0f) }
    val coefficient by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 10_000,
                easing = {
                    if (isAnimating) {
                        desiredEasing += 0.0016669631f

                        if (desiredEasing >= 1f) {
                            desiredEasing = 0f
                        }
                    }

                    desiredEasing
                },
            ),
        ),
        label = "ShapesBannerAnimation_Rotation",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                isAnimating = !isAnimating
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        polygons.forEach { roundedPolygon ->
            ColoredShape(
                polygon = roundedPolygon,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(polygonSize)
                    .graphicsLayer {
                        rotationZ = 360f * coefficient
                    },
            )
        }
    }
}

@Composable
@DevicePreviews
private fun ShapesBannerPreview() {
    ExtendedTheme {
        ShapesBanner()
    }
}
