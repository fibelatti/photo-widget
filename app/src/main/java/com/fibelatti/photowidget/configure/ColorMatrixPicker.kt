package com.fibelatti.photowidget.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.preferences.DefaultPicker
import com.fibelatti.photowidget.ui.AppBottomSheet
import com.fibelatti.photowidget.ui.AppSheetState
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.photowidget.ui.hideBottomSheet
import com.fibelatti.ui.foundation.dpToPx

@Composable
fun PhotoWidgetSaturationBottomSheet(
    sheetState: AppSheetState,
    currentSaturation: Float,
    onApplyClick: (Float) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        ColorMatrixPicker(
            title = stringResource(R.string.widget_defaults_saturation),
            valueRange = -100f..100f,
            currentValue = PhotoWidgetColors.pickerSaturation(currentSaturation),
            onCurrentValueChange = { value ->
                ColorMatrix().apply { setToSaturation(PhotoWidgetColors.persistenceSaturation(value) / 100) }
            },
            onApplyClick = { newValue ->
                onApplyClick(PhotoWidgetColors.persistenceSaturation(newValue))
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
fun PhotoWidgetBrightnessBottomSheet(
    sheetState: AppSheetState,
    currentBrightness: Float,
    onApplyClick: (Float) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        ColorMatrixPicker(
            title = stringResource(R.string.widget_defaults_brightness),
            valueRange = -100f..100f,
            currentValue = currentBrightness,
            onCurrentValueChange = { value ->
                val brightness = value * 255 / 100
                val colorMatrix = floatArrayOf(
                    1f, 0f, 0f, 0f, brightness,
                    0f, 1f, 0f, 0f, brightness,
                    0f, 0f, 1f, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f,
                )

                ColorMatrix(colorMatrix)
            },
            onApplyClick = { newValue ->
                onApplyClick(newValue)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun ColorMatrixPicker(
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    currentValue: Float,
    onCurrentValueChange: (Float) -> ColorMatrix,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = title,
        modifier = modifier,
    ) {
        var value by remember(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
            colorFilter = ColorFilter.colorMatrix(onCurrentValueChange(value)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                valueRange = valueRange,
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = formatRangeValue(value = value),
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
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
