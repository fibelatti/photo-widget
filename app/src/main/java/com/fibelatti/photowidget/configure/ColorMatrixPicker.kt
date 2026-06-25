package com.fibelatti.photowidget.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.SliderItem
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
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
private fun ColorMatrixPicker(
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    currentValue: Float,
    onCurrentValueChange: (Float) -> ColorMatrix,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = title,
        modifier = modifier,
    ) {
        var value by rememberSaveable(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
            colorFilter = ColorFilter.colorMatrix(onCurrentValueChange(value)),
        )

        SliderItem(
            value = value,
            valueText = formatRangeValue(value = value),
            onValueChange = { value = it },
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        DefaultSheetFooterButtons(
            onApplyClick = { onApplyClick(value) },
            onResetClick = { value = 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
