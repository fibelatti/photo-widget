package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.foundation.Shapes

@Composable
fun SliderItem(
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.StandaloneShape,
) {
    val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
    SideEffect(value.fastRoundToInt()) {
        localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight)
            .clip(shape),
        trailingContent = {
            SliderLabel(text = valueText)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        content = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                thumb = { SliderSmallThumb() },
            )
        },
    )
}

@Composable
fun SliderLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    AutoSizeText(
        text = text,
        modifier = modifier.width(60.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        style = MaterialTheme.typography.bodyMediumEmphasized,
    )
}
