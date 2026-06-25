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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.SliderItem
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.foundation.dpToPx

@Composable
fun OpacityPicker(
    currentValue: Float,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.widget_defaults_opacity),
        modifier = modifier,
    ) {
        var value by rememberSaveable(currentValue) { mutableFloatStateOf(currentValue) }

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

        SliderItem(
            value = value,
            valueText = formatPercent(value = value, fractionDigits = 0),
            onValueChange = { value = it },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        DefaultSheetFooterButtons(
            onApplyClick = { onApplyClick(value) },
            onResetClick = { value = 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
