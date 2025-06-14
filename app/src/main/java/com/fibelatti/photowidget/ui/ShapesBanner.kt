@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun ShapesBanner(
    modifier: Modifier = Modifier,
    polygonCount: Int = 5,
    polygonSize: Dp = 48.dp,
) {
    var isAnimating by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                isAnimating = !isAnimating
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (isAnimating) {
            val shapes = LoadingIndicator.Polygons.shuffled()
                .chunked(size = LoadingIndicator.Polygons.size / polygonCount)

            repeat(times = polygonCount) { index ->
                LoadingIndicator(
                    modifier = Modifier.size(polygonSize),
                    color = MaterialTheme.colorScheme.primary,
                    polygons = shapes[index],
                )
            }
        } else {
            val shapes = LoadingIndicator.Polygons.shuffled().take(polygonCount)

            repeat(times = shapes.size) { index ->
                Box(
                    modifier = Modifier
                        .size(polygonSize)
                        .padding(all = 4.dp)
                        .background(
                            shape = shapes[index].toShape(),
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
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
