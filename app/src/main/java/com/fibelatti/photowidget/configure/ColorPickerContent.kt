package com.fibelatti.photowidget.configure

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.fibelatti.ui.foundation.Shapes
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/**
 * An HSV color picker with a brightness slider and a hex input, next to a [preview] of the
 * selected color.
 *
 * @param currentColorHex the selected color, as a 6 character hex string without the `#` prefix.
 * @param onColorChange invoked with each new selection, in the same format as [currentColorHex].
 */
@Composable
fun ColorPickerContent(
    currentColorHex: String,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val colorPickerController: ColorPickerController = rememberColorPickerController()
        var colorHex: String by rememberSaveable { mutableStateOf(currentColorHex) }
        val hexChars: CharArray = remember {
            charArrayOf(
                '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                'a', 'b', 'c', 'd', 'e', 'f',
                'A', 'B', 'C', 'D', 'E', 'F',
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            preview()

            HsvColorPicker(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 200.dp)
                    .aspectRatio(1f),
                controller = colorPickerController,
                onColorChanged = { colorEnvelope ->
                    colorHex = colorEnvelope.hexCode.drop(2)
                    onColorChange(colorHex)
                },
                initialColor = Color("#$currentColorHex".toColorInt()),
            )
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrightnessSlider(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = Shapes.StandaloneShape,
                    ),
                controller = colorPickerController,
                borderRadius = Shapes.GroupCornerRadius,
                borderColor = Color.Transparent,
            )

            TextField(
                value = colorHex,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it in hexChars }) {
                        colorHex = newValue

                        if (colorHex.length == 6) {
                            val androidColor = "#$colorHex".toColorInt()
                            colorPickerController.selectByColor(color = Color(androidColor), fromUser = true)
                            onColorChange(colorHex)
                        }
                    }
                },
                modifier = Modifier.width(100.dp),
                textStyle = MaterialTheme.typography.labelMedium,
                placeholder = {
                    Text(
                        text = "FFFFFF",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                prefix = {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                shape = Shapes.StandaloneShape,
            )
        }
    }
}
