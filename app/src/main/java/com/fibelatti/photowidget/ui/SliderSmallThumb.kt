package com.fibelatti.photowidget.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun SliderSmallThumb(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = colors(),
    enabled: Boolean = true,
    thumbSize: DpSize = DpSize(width = 4.dp, 22.dp),
) {
    SliderDefaults.Thumb(
        interactionSource = interactionSource,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        thumbSize = thumbSize,
    )
}
