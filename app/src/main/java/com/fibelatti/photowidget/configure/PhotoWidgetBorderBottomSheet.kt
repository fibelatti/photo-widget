package com.fibelatti.photowidget.configure

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.ColumnToggleButtonGroup
import com.fibelatti.ui.foundation.ToggleButtonGroup
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun PhotoWidgetBorderBottomSheet(
    sheetState: AppSheetState,
    currentBorder: PhotoWidgetBorder,
    onApplyClick: (PhotoWidgetBorder) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        BorderPickerContent(
            currentBorder = currentBorder,
        ) { newBorder ->
            onApplyClick(newBorder)
            sheetState.hideBottomSheet()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun BorderPickerContent(
    currentBorder: PhotoWidgetBorder,
    onApplyClick: (PhotoWidgetBorder) -> Unit,
) {
    var border: PhotoWidgetBorder by remember { mutableStateOf(currentBorder) }
    val sampleBitmap = rememberSampleBitmap()

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

        ColumnToggleButtonGroup(
            items = PhotoWidgetBorder.entries.map {
                ToggleButtonGroup.Item(
                    id = it.serializedName,
                    text = stringResource(id = it.label),
                )
            },
            onButtonClick = { item ->
                border = PhotoWidgetBorder.fromSerializedName(item.id)
            },
            modifier = Modifier.fillMaxWidth(),
            selectedIndex = PhotoWidgetBorder.entries.indexOfFirst {
                it.serializedName == border.serializedName
            },
            colors = ToggleButtonGroup.colors(unselectedButtonColor = MaterialTheme.colorScheme.surfaceContainerLow),
            iconPosition = ToggleButtonGroup.IconPosition.End,
        )

        Spacer(modifier = Modifier.size(16.dp))

        when (val current = border) {
            is PhotoWidgetBorder.None -> Unit

            is PhotoWidgetBorder.Color -> {
                ColorBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentColorHex = current.colorHex,
                    onColorChange = { border = current.copy(colorHex = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                )
            }

            is PhotoWidgetBorder.Dynamic -> {
                DynamicBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                )
            }

            is PhotoWidgetBorder.MatchPhoto -> {
                MatchPhotoBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentType = current.type,
                    onTypeChange = { border = current.copy(type = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                )
            }
        }

        Button(
            onClick = { onApplyClick(border) },
            shapes = ButtonDefaults.shapes(),
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
    sampleBitmap: Bitmap,
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
                bitmap = sampleBitmap
                    .withRoundedCorners(
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
                initialColor = Color("#$currentColorHex".toColorInt()),
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
    sampleBitmap: Bitmap,
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

        Image(
            bitmap = sampleBitmap
                .withRoundedCorners(
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
private fun MatchPhotoBorderContent(
    sampleBitmap: Bitmap,
    currentType: PhotoWidgetBorder.MatchPhoto.Type,
    onTypeChange: (PhotoWidgetBorder.MatchPhoto.Type) -> Unit,
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val colorPalette = remember(sampleBitmap) { getColorPalette(sampleBitmap) }

        Image(
            bitmap = sampleBitmap
                .withRoundedCorners(
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                    borderColor = colorPalette.colorForType(currentType),
                    borderPercent = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR,
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        val radioTypes = PhotoWidgetBorder.MatchPhoto.Type.entries
        val (selectedType, onTypeSelected) = remember { mutableStateOf(currentType) }

        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )

        Column(
            modifier = Modifier.selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            radioTypes.forEach { type ->
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.large)
                        .selectable(
                            selected = (type == selectedType),
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            role = Role.RadioButton,
                            onClick = {
                                onTypeSelected(type)
                                onTypeChange(type)
                            },
                        )
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (type == selectedType),
                        onClick = null,
                    )
                    Text(
                        text = when (type) {
                            PhotoWidgetBorder.MatchPhoto.Type.DOMINANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_dominant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.VIBRANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_vibrant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.MUTED -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_muted)
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
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
        val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
        var value by remember { mutableIntStateOf(currentWidth) }
        RememberedEffect(value) {
            localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }

        Slider(
            value = currentWidth.toFloat(),
            onValueChange = {
                value = it.fastRoundToInt()
                onWidthChange(value)
            },
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
