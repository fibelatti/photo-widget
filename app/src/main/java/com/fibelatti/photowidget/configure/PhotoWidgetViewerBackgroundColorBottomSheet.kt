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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorPickerController
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

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
    var colorHex: String by rememberSaveable { mutableStateOf(initialColorHex) }
    val hexChars: CharArray = remember {
        charArrayOf(
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            'a', 'b', 'c', 'd', 'e', 'f',
            'A', 'B', 'C', 'D', 'E', 'F',
        )
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
                    Image(
                        bitmap = sampleBitmap
                            .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                            .asImageBitmap(),
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
                        colorHex = colorEnvelope.hexCode.drop(2)
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
                            shape = RoundedCornerShape(6.dp),
                        ),
                    controller = colorPickerController,
                )

                TextField(
                    value = colorHex,
                    onValueChange = { newValue ->
                        if (newValue.length <= 6 && newValue.all { it in hexChars }) {
                            colorHex = newValue

                            if (colorHex.length == 6) {
                                val androidColor = "#$colorHex".toColorInt()
                                colorPickerController.selectByColor(color = Color(androidColor), fromUser = true)
                            }
                        }
                    },
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
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
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
