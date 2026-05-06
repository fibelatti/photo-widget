package com.fibelatti.photowidget.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.GifFrames
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetGifIntervalBottomSheet(
    sheetState: AppSheetState,
    gifInterval: Long,
    onApplyClick: (newInterval: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        PhotoWidgetGifIntervalContent(
            gifInterval = gifInterval,
            onApplyClick = { newValue: Long ->
                onApplyClick(newValue)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun PhotoWidgetGifIntervalContent(
    gifInterval: Long,
    onApplyClick: (newInterval: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_configure_gif_frame_interval),
        modifier = modifier,
    ) {
        var value: Long by remember(gifInterval) { mutableLongStateOf(gifInterval) }

        Text(
            text = stringResource(R.string.photo_widget_configure_gif_frame_interval_description),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
            SideEffect(value) {
                localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { value = it.fastRoundToInt().toLong() },
                modifier = Modifier.weight(1f),
                valueRange = GifFrames.MIN_INTERVAL_MS.toFloat()..GifFrames.MAX_INTERVAL_MS.toFloat(),
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = "$value ms",
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.photo_widget_configure_gif_frame_interval_presets),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )

            val presets: Map<String, Long> = remember {
                mapOf(
                    "10 fps" to 100,
                    "12 fps" to 83,
                    "15 fps" to 66,
                    "24 fps" to 41,
                    "30 fps" to 33,
                    "50 fps" to 20,
                )
            }

            for (item in presets) {
                OutlinedButton(
                    onClick = { value = item.value },
                ) {
                    Text(text = item.key)
                }
            }
        }

        Button(
            onClick = { onApplyClick(value) },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@PreviewAll
@Composable
private fun PhotoWidgetGifIntervalContentPreview() {
    ExtendedTheme {
        PhotoWidgetGifIntervalContent(
            gifInterval = 66,
            onApplyClick = {},
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
        )
    }
}
