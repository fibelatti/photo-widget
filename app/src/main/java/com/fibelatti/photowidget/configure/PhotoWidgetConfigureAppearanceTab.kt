package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.preferences.CornerRadiusPicker
import com.fibelatti.photowidget.preferences.OpacityPicker
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.preferences.ShapeDefault
import com.fibelatti.photowidget.preferences.ShapePicker
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetConfigureAppearanceTab(
    viewModel: PhotoWidgetConfigureViewModel,
    modifier: Modifier = Modifier,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    val aspectRatioPickerSheetState: AppSheetState = rememberAppSheetState()
    val shapePickerSheetState: AppSheetState = rememberAppSheetState()
    val cornerRadiusPickerSheetState: AppSheetState = rememberAppSheetState()
    val borderPickerSheetState: AppSheetState = rememberAppSheetState()
    val opacityPickerSheetState: AppSheetState = rememberAppSheetState()
    val saturationPickerSheetState: AppSheetState = rememberAppSheetState()
    val brightnessPickerSheetState: AppSheetState = rememberAppSheetState()
    val offsetPickerSheetState: AppSheetState = rememberAppSheetState()
    val paddingPickerSheetState: AppSheetState = rememberAppSheetState()

    PhotoWidgetConfigureAppearanceTab(
        photoWidget = state.photoWidget,
        onAspectRatioClick = aspectRatioPickerSheetState::showBottomSheet,
        onShapeClick = shapePickerSheetState::showBottomSheet,
        onCornerRadiusClick = cornerRadiusPickerSheetState::showBottomSheet,
        onBorderClick = borderPickerSheetState::showBottomSheet,
        onOpacityClick = opacityPickerSheetState::showBottomSheet,
        onSaturationClick = saturationPickerSheetState::showBottomSheet,
        onBrightnessClick = brightnessPickerSheetState::showBottomSheet,
        onOffsetClick = offsetPickerSheetState::showBottomSheet,
        onPaddingClick = paddingPickerSheetState::showBottomSheet,
        modifier = modifier,
    )

    // region Sheets
    PhotoWidgetAspectRatioBottomSheet(
        sheetState = aspectRatioPickerSheetState,
        onAspectRatioSelected = viewModel::setAspectRatio,
    )

    AppBottomSheet(
        sheetState = shapePickerSheetState,
    ) {
        ShapePicker(
            onClick = { newShapeId ->
                viewModel.shapeSelected(newShapeId)
                shapePickerSheetState.hideBottomSheet()
            },
            selectedShapeId = state.photoWidget.shapeId,
        )
    }

    AppBottomSheet(
        sheetState = cornerRadiusPickerSheetState,
    ) {
        CornerRadiusPicker(
            currentValue = state.photoWidget.cornerRadius,
            onApplyClick = { newValue ->
                viewModel.cornerRadiusSelected(newValue)
                cornerRadiusPickerSheetState.hideBottomSheet()
            },
        )
    }

    PhotoWidgetBorderBottomSheet(
        sheetState = borderPickerSheetState,
        currentBorder = state.photoWidget.border,
        onApplyClick = viewModel::borderSelected,
    )

    AppBottomSheet(
        sheetState = opacityPickerSheetState,
    ) {
        OpacityPicker(
            currentValue = state.photoWidget.colors.opacity,
            onApplyClick = { newValue ->
                viewModel.opacitySelected(newValue)
                opacityPickerSheetState.hideBottomSheet()
            },
        )
    }

    PhotoWidgetSaturationBottomSheet(
        sheetState = saturationPickerSheetState,
        currentSaturation = state.photoWidget.colors.saturation,
        onApplyClick = viewModel::saturationSelected,
    )

    PhotoWidgetBrightnessBottomSheet(
        sheetState = brightnessPickerSheetState,
        currentBrightness = state.photoWidget.colors.brightness,
        onApplyClick = viewModel::brightnessSelected,
    )

    AppBottomSheet(
        sheetState = offsetPickerSheetState,
    ) {
        PhotoWidgetOffsetPicker(
            horizontalOffset = state.photoWidget.horizontalOffset,
            verticalOffset = state.photoWidget.verticalOffset,
            onApplyClick = { newHorizontalOffset, newVerticalOffset ->
                viewModel.offsetSelected(newHorizontalOffset, newVerticalOffset)
                offsetPickerSheetState.hideBottomSheet()
            },
        )
    }

    AppBottomSheet(
        sheetState = paddingPickerSheetState,
    ) {
        PhotoWidgetPaddingPicker(
            currentValue = state.photoWidget.padding,
            onApplyClick = { newValue ->
                viewModel.paddingSelected(newValue)
                paddingPickerSheetState.hideBottomSheet()
            },
        )
    }
    // endregion Sheets
}

@Composable
fun PhotoWidgetConfigureAppearanceTab(
    photoWidget: PhotoWidget,
    onAspectRatioClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickerDefault(
            title = stringResource(id = R.string.photo_widget_aspect_ratio_title),
            currentValue = stringResource(id = photoWidget.aspectRatio.label),
            onClick = onAspectRatioClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
            ShapeDefault(
                title = stringResource(id = R.string.widget_defaults_shape),
                currentValue = photoWidget.shapeId,
                onClick = onShapeClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_corner_radius),
                currentValue = photoWidget.cornerRadius.toString(),
                onClick = onCornerRadiusClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(R.string.photo_widget_configure_border),
                currentValue = when (photoWidget.border) {
                    is PhotoWidgetBorder.None -> stringResource(id = R.string.photo_widget_configure_border_none)
                    is PhotoWidgetBorder.Color -> "#${photoWidget.border.colorHex}".toUpperCase(Locale.current)
                    is PhotoWidgetBorder.Dynamic -> stringResource(R.string.photo_widget_configure_border_dynamic)
                    is PhotoWidgetBorder.MatchPhoto -> {
                        when (photoWidget.border.type) {
                            PhotoWidgetBorder.MatchPhoto.Type.DOMINANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_dominant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.VIBRANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_vibrant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.MUTED -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_muted)
                            }
                        }
                    }
                },
                onClick = onBorderClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_opacity),
            currentValue = formatPercent(value = photoWidget.colors.opacity, fractionDigits = 0),
            onClick = onOpacityClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PickerDefault(
            title = stringResource(R.string.widget_defaults_saturation),
            currentValue = formatRangeValue(value = PhotoWidgetColors.pickerSaturation(photoWidget.colors.saturation)),
            onClick = onSaturationClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PickerDefault(
            title = stringResource(R.string.widget_defaults_brightness),
            currentValue = formatRangeValue(value = photoWidget.colors.brightness),
            onClick = onBrightnessClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(id = R.string.photo_widget_configure_offset),
                currentValue = stringResource(
                    id = R.string.photo_widget_configure_offset_current_values,
                    photoWidget.horizontalOffset,
                    photoWidget.verticalOffset,
                ),
                onClick = onOffsetClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            PickerDefault(
                title = stringResource(id = R.string.photo_widget_configure_padding),
                currentValue = photoWidget.padding.toString(),
                onClick = onPaddingClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// region Previews
@AllPreviews
@Composable
private fun PhotoWidgetConfigureAppearanceTabPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureAppearanceTab(
            photoWidget = PhotoWidget(
                photos = List(10) { index -> LocalPhoto(photoId = "photo-$index") },
                aspectRatio = PhotoWidgetAspectRatio.ROUNDED_SQUARE,
            ),
            onAspectRatioClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onBorderClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onOffsetClick = {},
            onPaddingClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@AllPreviews
@Composable
private fun PhotoWidgetConfigureAppearanceTabShapePreview() {
    ExtendedTheme {
        PhotoWidgetConfigureAppearanceTab(
            photoWidget = PhotoWidget(
                photos = List(10) { index -> LocalPhoto(photoId = "photo-$index") },
                aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            ),
            onAspectRatioClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onBorderClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onOffsetClick = {},
            onPaddingClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@AllPreviews
@Composable
private fun PhotoWidgetConfigureAppearanceTabFillPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureAppearanceTab(
            photoWidget = PhotoWidget(
                photos = List(10) { index -> LocalPhoto(photoId = "photo-$index") },
                aspectRatio = PhotoWidgetAspectRatio.FILL_WIDGET,
            ),
            onAspectRatioClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onBorderClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onOffsetClick = {},
            onPaddingClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
// endregion Previews
