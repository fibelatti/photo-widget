package com.fibelatti.photowidget.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.foundation.dpToPx

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OpacityPicker(
    currentValue: Float,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.widget_defaults_opacity),
        modifier = modifier,
    ) {
        var value by remember(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                    colors = PhotoWidgetColors(opacity = value),
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
            RememberedEffect(value.fastRoundToInt()) {
                localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }

            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                valueRange = 0f..100f,
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = formatPercent(value = value, fractionDigits = 0),
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        DefaultSheetFooterButtons(
            onApplyClick = { onApplyClick(value) },
            onResetClick = { value = 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
