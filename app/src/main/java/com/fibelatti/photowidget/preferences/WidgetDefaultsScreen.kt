package com.fibelatti.photowidget.preferences

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.ColoredShape
import com.fibelatti.photowidget.configure.PhotoWidgetIntervalPicker
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit

@Composable
fun WidgetDefaultsScreen(
    preferencesViewModel: WidgetDefaultsViewModel = hiltViewModel(),
    onNavClick: () -> Unit,
) {
    val preferences by preferencesViewModel.userPreferences.collectAsStateWithLifecycle()
    val localContext = LocalContext.current

    WidgetDefaultsScreen(
        userPreferences = preferences,
        onNavClick = onNavClick,
        onSourceClick = {
            SelectionDialog.show(
                context = localContext,
                title = localContext.getString(R.string.widget_defaults_source),
                options = PhotoWidgetSource.entries,
                optionName = { option -> localContext.getString(option.label) },
                onOptionSelected = preferencesViewModel::saveDefaultSource,
            )
        },
        onShuffleChange = preferencesViewModel::saveDefaultShuffle,
        onIntervalClick = {
            PhotoWidgetIntervalPicker.show(
                context = localContext,
                currentInterval = preferences.defaultInterval,
                currentIntervalBasedLoopingEnabled = preferences.defaultIntervalEnabled,
                onApplyClick = { newInterval, intervalBasedLoopingEnabled ->
                    preferencesViewModel.saveDefaultInterval(newInterval)
                    preferencesViewModel.saveDefaultIntervalEnabled(intervalBasedLoopingEnabled)
                },
            )
        },
        onShapeClick = {
            ComposeBottomSheetDialog(localContext) {
                ShapePicker(
                    onClick = { newShapeId ->
                        preferencesViewModel.saveDefaultShape(newShapeId)
                        dismiss()
                    },
                )
            }.show()
        },
        onCornerRadiusClick = {
            ComposeBottomSheetDialog(localContext) {
                CornerRadiusPicker(
                    currentValue = preferences.defaultCornerRadius,
                    onApplyClick = { newValue ->
                        preferencesViewModel.saveDefaultCornerRadius(newValue)
                        dismiss()
                    },
                )
            }.show()
        },
        onOpacityClick = {
            ComposeBottomSheetDialog(localContext) {
                OpacityPicker(
                    currentValue = preferences.defaultOpacity,
                    onApplyClick = { newValue ->
                        preferencesViewModel.saveDefaultOpacity(newValue)
                        dismiss()
                    },
                )
            }.show()
        },
        onTapActionClick = {
            SelectionDialog.show(
                context = localContext,
                title = localContext.getString(R.string.widget_defaults_tap_action),
                options = PhotoWidgetTapAction.entries,
                optionName = { option -> localContext.getString(option.label) },
                onOptionSelected = preferencesViewModel::saveDefaultTapAction,
            )
        },
        onIncreaseBrightnessChange = preferencesViewModel::saveDefaultIncreaseBrightness,
        onClearDefaultsClick = preferencesViewModel::clearDefaults,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WidgetDefaultsScreen(
    userPreferences: UserPreferences,
    onNavClick: () -> Unit,
    onSourceClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onIntervalClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onTapActionClick: () -> Unit,
    onIncreaseBrightnessChange: (Boolean) -> Unit,
    onClearDefaultsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.widget_defaults_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.widget_defaults_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_source),
                currentValue = stringResource(id = userPreferences.defaultSource.label),
                onClick = onSourceClick,
            )

            BooleanDefault(
                title = stringResource(id = R.string.widget_defaults_shuffle),
                currentValue = userPreferences.defaultShuffle,
                onCheckedChange = onShuffleChange,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_interval),
                currentValue = if (userPreferences.defaultIntervalEnabled) {
                    val intervalString = pluralStringResource(
                        id = when (userPreferences.defaultInterval.timeUnit) {
                            TimeUnit.SECONDS -> R.plurals.photo_widget_configure_interval_current_seconds
                            TimeUnit.MINUTES -> R.plurals.photo_widget_configure_interval_current_minutes
                            else -> R.plurals.photo_widget_configure_interval_current_hours
                        },
                        count = userPreferences.defaultInterval.repeatInterval.toInt(),
                        userPreferences.defaultInterval.repeatInterval,
                    )
                    stringResource(id = R.string.photo_widget_configure_interval_current_label, intervalString)
                } else {
                    stringResource(id = R.string.photo_widget_configure_interval_current_disabled)
                },
                onClick = onIntervalClick,
            )

            ShapeDefault(
                title = stringResource(id = R.string.widget_defaults_shape),
                currentValue = userPreferences.defaultShape,
                onClick = onShapeClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_corner_radius),
                currentValue = userPreferences.defaultCornerRadius.toInt().toString(),
                onClick = onCornerRadiusClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_opacity),
                currentValue = userPreferences.defaultOpacity.toInt().toString(),
                onClick = onOpacityClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_tap_action),
                currentValue = stringResource(id = userPreferences.defaultTapAction.label),
                onClick = onTapActionClick,
            )

            BooleanDefault(
                title = stringResource(id = R.string.widget_defaults_increase_brightness),
                currentValue = userPreferences.defaultIncreaseBrightness,
                onCheckedChange = onIncreaseBrightnessChange,
            )

            OutlinedButton(
                onClick = onClearDefaultsClick,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(text = stringResource(id = R.string.widget_defaults_reset))
            }
        }
    }
}

// region Items
@Composable
private fun BooleanDefault(
    title: String,
    currentValue: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors().run {
            copy(containerColor = containerColor.copy(alpha = 0.6f))
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = currentValue,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
fun PickerDefault(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors().run {
            copy(containerColor = containerColor.copy(alpha = 0.6f))
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ShapeDefault(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors().run {
            copy(containerColor = containerColor.copy(alpha = 0.6f))
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.weight(1f))

            ColoredShape(
                polygon = PhotoWidgetShapeBuilder.buildShape(shapeId = currentValue),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
// endregion Items

// region Pickers
@Composable
fun ShapePicker(
    onClick: (shapeId: String) -> Unit,
    modifier: Modifier = Modifier,
    selectedShapeId: String? = null,
) {
    DefaultPicker(
        title = stringResource(id = R.string.widget_defaults_shape),
        modifier = modifier,
    ) {
        val shapesToPolygons = remember {
            PhotoWidgetShapeBuilder.buildAllShapes().toList()
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shapesToPolygons) { (shape, polygon) ->
                val color by animateColorAsState(
                    targetValue = if (shape.id == selectedShapeId || selectedShapeId == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "ShapePicker_SelectedColor",
                )
                ColoredShape(
                    polygon = polygon,
                    color = color,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio = 1f)
                        .clickable { onClick(shape.id) },
                )
            }
        }
    }
}

@Composable
fun CornerRadiusPicker(
    currentValue: Float,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.widget_defaults_corner_radius),
        modifier = modifier,
    ) {
        val localContext = LocalContext.current
        val baseBitmap = remember {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
        }
        var value by remember(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = baseBitmap
                .withRoundedCorners(
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    radius = value,
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                valueRange = 0f..100f,
            )

            Text(
                text = "${value.toInt()}",
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FilledTonalButton(
            onClick = { onApplyClick(value) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@Composable
fun OpacityPicker(
    currentValue: Float,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.widget_defaults_opacity),
        modifier = modifier,
    ) {
        val localContext = LocalContext.current
        val baseBitmap = remember {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
        }
        var value by remember(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = baseBitmap
                .withRoundedCorners(
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                    opacity = value,
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
                valueRange = 0f..100f,
            )

            Text(
                text = "${value.toInt()}",
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FilledTonalButton(
            onClick = { onApplyClick(value) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

@Composable
fun DefaultPicker(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        content()
    }
}
// endregion Pickers

// region Previews
@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun WidgetDefaultsScreenPreview() {
    ExtendedTheme {
        WidgetDefaultsScreen(
            userPreferences = UserPreferences(
                appearance = Appearance.FOLLOW_SYSTEM,
                dynamicColors = true,
                defaultSource = PhotoWidgetSource.PHOTOS,
                defaultShuffle = false,
                defaultIntervalEnabled = false,
                defaultInterval = PhotoWidgetLoopingInterval.ONE_DAY,
                defaultShape = PhotoWidget.DEFAULT_SHAPE_ID,
                defaultCornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                defaultOpacity = PhotoWidget.DEFAULT_OPACITY,
                defaultTapAction = PhotoWidgetTapAction.NONE,
                defaultIncreaseBrightness = true,
            ),
            onNavClick = {},
            onSourceClick = {},
            onShuffleChange = {},
            onIntervalClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onOpacityClick = {},
            onTapActionClick = {},
            onIncreaseBrightnessChange = {},
            onClearDefaultsClick = {},
        )
    }
}

@Composable
@ThemePreviews
private fun PickerDefaultPreview() {
    ExtendedTheme {
        PickerDefault(
            title = "Some default",
            currentValue = "Some value",
            onClick = {},
        )
    }
}

@Composable
@ThemePreviews
private fun BooleanDefaultPreview() {
    ExtendedTheme {
        BooleanDefault(
            title = "Some default",
            currentValue = true,
            onCheckedChange = {},
        )
    }
}
// endregion Previews
