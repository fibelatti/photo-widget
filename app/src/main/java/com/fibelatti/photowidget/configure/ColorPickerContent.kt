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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

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
        val colorHexTextState: TextFieldState = rememberTextFieldState(currentColorHex)
        // The last hex reported by the picker, so that echoing it into the text field doesn't push it back
        var pickerColorHex: String? by remember { mutableStateOf(null) }
        val currentOnColorChange: (String) -> Unit by rememberUpdatedState(onColorChange)

        LaunchedEffect(colorHexTextState, colorPickerController) {
            snapshotFlow { colorHexTextState.text.toString() }
                .drop(1)
                .filter { hex -> hex.length == 6 && !hex.equals(pickerColorHex, ignoreCase = true) }
                .collect { hex ->
                    colorPickerController.selectByColor(color = Color("#$hex".toColorInt()), fromUser = true)
                    currentOnColorChange(hex)
                }
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
                    val hex: String = colorEnvelope.hexCode.drop(2)
                    pickerColorHex = hex
                    colorHexTextState.setTextAndPlaceCursorAtEnd(hex)
                    currentOnColorChange(hex)
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
                state = colorHexTextState,
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
                inputTransformation = HexColorInputTransformation,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = Shapes.StandaloneShape,
                colors = TextFieldDefaults.tonalColors(),
            )
        }
    }
}
