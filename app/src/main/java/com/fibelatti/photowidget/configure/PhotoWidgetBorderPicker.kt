package com.fibelatti.photowidget.configure

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.android.material.color.DynamicColors
import kotlin.math.roundToInt

object PhotoWidgetBorderPicker {

    fun show(
        context: Context,
        currentBorder: PhotoWidgetBorder,
        onApplyClick: (PhotoWidgetBorder) -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            BorderPickerContent(
                currentBorder = currentBorder,
            ) { newBorder ->
                onApplyClick(newBorder)
                dismiss()
            }
        }.show()
    }
}

@Composable
private fun BorderPickerContent(
    currentBorder: PhotoWidgetBorder,
    onApplyClick: (PhotoWidgetBorder) -> Unit,
) {
    var border: PhotoWidgetBorder by remember { mutableStateOf(currentBorder) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_configure_border),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.size(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val buttonBorderColor = SegmentedButtonDefaults.borderStroke(
                color = SegmentedButtonDefaults.colors().activeBorderColor,
            )

            SegmentedButton(
                selected = border is PhotoWidgetBorder.None,
                onClick = {
                    border = PhotoWidgetBorder.None
                },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = buttonBorderColor,
                label = {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_border_none),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            if (DynamicColors.isDynamicColorAvailable()) {
                SegmentedButton(
                    selected = border is PhotoWidgetBorder.Dynamic,
                    onClick = {
                        border = PhotoWidgetBorder.Dynamic(width = PhotoWidgetBorder.DEFAULT_WIDTH)
                    },
                    shape = RectangleShape,
                    border = buttonBorderColor,
                    label = {
                        Text(
                            text = stringResource(R.string.photo_widget_configure_border_dynamic),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
            }

            SegmentedButton(
                selected = border is PhotoWidgetBorder.Color,
                onClick = {
                    border = PhotoWidgetBorder.Color(colorHex = "ffffff", width = PhotoWidgetBorder.DEFAULT_WIDTH)
                },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                border = buttonBorderColor,
                label = {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_border_color),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        val current = border
        @Suppress("IntroduceWhenSubject")
        when {
            current is PhotoWidgetBorder.Color -> {
                ColorBorderContent(
                    currentColorHex = current.colorHex,
                    onColorChange = { border = current.copy(colorHex = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                )
            }

            current is PhotoWidgetBorder.Dynamic -> {
                DynamicBorderContent(
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                )
            }
        }

        FilledTonalButton(
            onClick = { onApplyClick(border) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.photo_widget_action_apply),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ColorBorderContent(
    currentColorHex: String,
    onColorChange: (String) -> Unit,
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val localContext = LocalContext.current
        val baseBitmap = remember {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
        }
        val colorPickerController = rememberColorPickerController()
        var colorHex by remember { mutableStateOf(currentColorHex) }
        val hexChars = remember {
            charArrayOf(
                '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                'a', 'b', 'c', 'd', 'e', 'f',
                'A', 'B', 'C', 'D', 'E', 'F',
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = baseBitmap
                    .withRoundedCorners(
                        aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                        radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                        borderColor = "#$currentColorHex".toColorInt(),
                        borderPercent = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR,
                    )
                    .asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
            )

            HsvColorPicker(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                controller = colorPickerController,
                onColorChanged = { colorEnvelope ->
                    colorHex = colorEnvelope.hexCode.drop(2)
                    onColorChange(colorHex)
                },
                initialColor = Color(android.graphics.Color.parseColor("#$currentColorHex")),
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
                            val androidColor = android.graphics.Color.parseColor("#$colorHex")
                            colorPickerController.selectByColor(color = Color(androidColor), fromUser = true)
                            onColorChange(colorHex)
                        }
                    }
                },
                modifier = Modifier.width(100.dp),
                textStyle = MaterialTheme.typography.labelMedium,
                placeholder = {
                    Text(
                        text = "ffffff",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                prefix = {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                maxLines = 1,
            )
        }

        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )
    }
}

@Composable
private fun DynamicBorderContent(
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val localContext = LocalContext.current
        val baseBitmap = remember {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
        }

        Image(
            bitmap = baseBitmap
                .withRoundedCorners(
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                    borderColor = localContext.getDynamicAttributeColor(
                        com.google.android.material.R.attr.colorPrimaryInverse,
                    ),
                    borderPercent = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR,
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )

        Text(
            text = stringResource(R.string.photo_widget_configure_border_explanation),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BorderWidthPicker(
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Slider(
            value = currentWidth.toFloat(),
            onValueChange = { onWidthChange(it.roundToInt()) },
            modifier = Modifier.weight(1f),
            valueRange = PhotoWidgetBorder.VALUE_RANGE,
            thumb = { SliderSmallThumb() },
        )

        Text(
            text = formatPercent(value = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR * 100),
            modifier = Modifier.width(60.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// region Previews
@Composable
@ThemePreviews
private fun ColorBorderPickerContentPreview() {
    ExtendedTheme {
        BorderPickerContent(
            currentBorder = PhotoWidgetBorder.Color(
                colorHex = "86D986",
                width = PhotoWidgetBorder.DEFAULT_WIDTH,
            ),
            onApplyClick = {},
        )
    }
}

@Composable
@ThemePreviews
private fun DynamicBorderPickerContentPreview() {
    ExtendedTheme {
        BorderPickerContent(
            currentBorder = PhotoWidgetBorder.Dynamic(
                width = PhotoWidgetBorder.DEFAULT_WIDTH,
            ),
            onApplyClick = {},
        )
    }
}
// endregion Previews
