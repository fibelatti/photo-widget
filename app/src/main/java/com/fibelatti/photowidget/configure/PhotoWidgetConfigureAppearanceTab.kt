package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.preferences.ShapeDefault

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
