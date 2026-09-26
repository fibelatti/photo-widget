package com.fibelatti.photowidget.configure

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.DynamicColorType
import com.fibelatti.photowidget.model.MatchPhotoColorType
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.RadioGroup
import com.fibelatti.ui.component.SliderItem
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.PreviewThemesAndColors
import com.fibelatti.ui.theme.ExtendedTheme

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
private fun BorderPickerContent(
    currentBorder: PhotoWidgetBorder,
    onApplyClick: (PhotoWidgetBorder) -> Unit,
) {
    var border: PhotoWidgetBorder by rememberSaveable { mutableStateOf(currentBorder) }
    val sampleBitmap = rememberSampleBitmap()
    val localResources = LocalResources.current

    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_configure_border),
        modifier = Modifier.animateContentSize(),
    ) {
        RadioGroup(
            items = PhotoWidgetBorder.entries,
            itemSelected = { item -> item.serializedName == border.serializedName },
            onItemClick = { item ->
                border = if (item.serializedName == currentBorder.serializedName) {
                    currentBorder
                } else {
                    item
                }
            },
            itemTitle = { item -> localResources.getString(item.label) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when (val current = border) {
            is PhotoWidgetBorder.None -> Unit

            is PhotoWidgetBorder.Color -> {
                ColorBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentColorHex = current.colorHex,
                    onColorChange = { border = current.copy(colorHex = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            is PhotoWidgetBorder.Dynamic -> {
                DynamicBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentType = current.type,
                    onTypeChange = { border = current.copy(type = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            is PhotoWidgetBorder.MatchPhoto -> {
                MatchPhotoBorderContent(
                    sampleBitmap = sampleBitmap,
                    currentType = current.type,
                    onTypeChange = { border = current.copy(type = it) },
                    currentWidth = current.width,
                    onWidthChange = { border = current.copy(width = it) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Button(
            onClick = { onApplyClick(border) },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
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
        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )

        ColorPickerContent(
            currentColorHex = currentColorHex,
            onColorChange = onColorChange,
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
                    .weight(1f, fill = false)
                    .widthIn(max = 200.dp)
                    .aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun DynamicBorderContent(
    sampleBitmap: Bitmap,
    currentType: DynamicColorType,
    onTypeChange: (DynamicColorType) -> Unit,
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val localContext = LocalContext.current
            val localResources = LocalResources.current
            val radioTypes = DynamicColorType.entries
            val (selectedType, onTypeSelected) = rememberSaveable { mutableStateOf(currentType) }

            Image(
                bitmap = sampleBitmap
                    .withRoundedCorners(
                        radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                        borderColor = localContext.getDynamicAttributeColor(currentType.colorAttr),
                        borderPercent = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR,
                    )
                    .asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .widthIn(max = min(200.dp, LocalWindowInfo.current.containerDpSize.width / 2))
                    .aspectRatio(1f),
            )

            RadioGroup(
                items = radioTypes,
                itemSelected = { type -> type == selectedType },
                onItemClick = { type ->
                    onTypeSelected(type)
                    onTypeChange(type)
                },
                itemTitle = { type -> localResources.getString(type.label) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.photo_widget_configure_color_dynamic_explanation),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MatchPhotoBorderContent(
    sampleBitmap: Bitmap,
    currentType: MatchPhotoColorType,
    onTypeChange: (MatchPhotoColorType) -> Unit,
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BorderWidthPicker(
            currentWidth = currentWidth,
            onWidthChange = onWidthChange,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val colorPalette = remember(sampleBitmap) { getColorPalette(sampleBitmap) }
            val radioTypes = MatchPhotoColorType.entries
            val (selectedType, onTypeSelected) = rememberSaveable { mutableStateOf(currentType) }
            val localResources = LocalResources.current

            Image(
                bitmap = sampleBitmap
                    .withRoundedCorners(
                        radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                        borderColor = colorPalette.colorForType(currentType),
                        borderPercent = currentWidth * PhotoWidgetBorder.PERCENT_FACTOR,
                    )
                    .asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .widthIn(max = min(200.dp, LocalWindowInfo.current.containerDpSize.width / 2))
                    .aspectRatio(1f),
            )

            RadioGroup(
                items = radioTypes,
                itemSelected = { type -> type == selectedType },
                onItemClick = { type ->
                    onTypeSelected(type)
                    onTypeChange(type)
                },
                itemTitle = { type -> localResources.getString(type.label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BorderWidthPicker(
    currentWidth: Int,
    onWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderState: SliderState = rememberSliderState(
        value = currentWidth.toFloat(),
        trackRange = PhotoWidgetBorder.VALUE_RANGE,
    )

    SliderItem(
        state = sliderState,
        displayValueTransformation = {
            formatPercent(value = it.fastRoundToInt() * PhotoWidgetBorder.PERCENT_FACTOR * 100)
        },
        modifier = modifier.fillMaxWidth(),
        onValueChange = { newValue -> onWidthChange(newValue.fastRoundToInt()) },
    )
}

// region Previews
@Composable
@PreviewThemesAndColors
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
@PreviewThemesAndColors
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
