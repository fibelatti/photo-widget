package com.fibelatti.photowidget.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.SliderItem
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.foundation.dpToPx

@Composable
fun CornerRadiusPicker(
    currentValue: Int,
    onApplyClick: (newValue: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.widget_defaults_corner_radius),
        modifier = modifier,
    ) {
        var value by rememberSaveable(currentValue) { mutableIntStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = value.dpToPx())
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        SliderItem(
            value = value.toFloat(),
            valueText = "$value",
            onValueChange = { value = it.fastRoundToInt() },
            valueRange = 0f..128f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

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
