package com.fibelatti.photowidget.configure

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

private const val FALLBACK_COLOR_HEX = "000000"

@Composable
fun PhotoWidgetViewerBackgroundColorBottomSheet(
    sheetState: AppSheetState,
    currentColorHex: String?,
    onApplyClick: (String) -> Unit,
    onResetClick: () -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        ViewerBackgroundColorContent(
            currentColorHex = currentColorHex,
            onApplyClick = { hex ->
                onApplyClick(hex)
                sheetState.hideBottomSheet()
            },
            onResetClick = {
                onResetClick()
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
private fun ViewerBackgroundColorContent(
    currentColorHex: String?,
    onApplyClick: (String) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialColorHex: String = currentColorHex ?: FALLBACK_COLOR_HEX
    val colorPickerController: ColorPickerController = rememberColorPickerController()
    val colorHexTextState: TextFieldState = rememberTextFieldState(initialColorHex)
    val colorHex: String = colorHexTextState.text.toString()
    // The last hex reported by the picker, so that echoing it into the text field doesn't push it back
    var pickerColorHex: String? by remember { mutableStateOf(null) }

    LaunchedEffect(colorHexTextState, colorPickerController) {
        snapshotFlow { colorHexTextState.text.toString() }
            .drop(1)
            .filter { hex -> hex.length == 6 && !hex.equals(pickerColorHex, ignoreCase = true) }
            .collect { hex ->
                colorPickerController.selectByColor(color = Color("#$hex".toColorInt()), fromUser = true)
            }
    }
    val sampleBitmap: Bitmap = rememberSampleBitmap()

    val previewBackgroundColor: Color = remember(colorHex) {
        if (colorHex.length == 6) {
            runCatching { Color("#$colorHex".toColorInt()) }.getOrNull()
        } else {
            null
        }
    } ?: Color("#$initialColorHex".toColorInt())

    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_configure_tap_action_viewer_background_color),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 200.dp)
                        .aspectRatio(1f)
                        .background(color = previewBackgroundColor, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    val cornerRadius: Float = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx()

                    Image(
                        bitmap = remember(sampleBitmap, cornerRadius) {
                            sampleBitmap.withRoundedCorners(radius = cornerRadius).asImageBitmap()
                        },
                        contentDescription = null,
                    )
                }

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
                    },
                    initialColor = Color("#$initialColorHex".toColorInt()),
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
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
                )

                TextField(
                    state = colorHexTextState,
                    modifier = Modifier.width(100.dp),
                    textStyle = MaterialTheme.typography.labelMedium,
                    placeholder = {
                        Text(
                            text = "000000",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onResetClick,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    AutoSizeText(
                        text = stringResource(
                            R.string.photo_widget_configure_tap_action_viewer_background_color_default,
                        ),
                        maxLines = 1,
                    )
                }

                Button(
                    onClick = { onApplyClick(colorHex) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                    enabled = colorHex.length == 6,
                ) {
                    Text(text = stringResource(R.string.photo_widget_action_apply))
                }
            }
        }
    }
}

@PreviewAll
@Composable
private fun ViewerBackgroundColorContentPreview() {
    ExtendedTheme {
        ViewerBackgroundColorContent(
            currentColorHex = null,
            onApplyClick = {},
            onResetClick = {},
        )
    }
}
