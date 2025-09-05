@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.preferences

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.DirectorySortingBottomSheet
import com.fibelatti.photowidget.configure.PhotoWidgetAspectRatioPicker
import com.fibelatti.photowidget.configure.PhotoWidgetBrightnessPicker
import com.fibelatti.photowidget.configure.PhotoWidgetCycleModePicker
import com.fibelatti.photowidget.configure.PhotoWidgetSaturationPicker
import com.fibelatti.photowidget.configure.rememberSampleBitmap
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.photowidget.ui.rememberAppSheetState
import com.fibelatti.photowidget.ui.showBottomSheet
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun WidgetDefaultsScreen(
    preferencesViewModel: WidgetDefaultsViewModel = hiltViewModel(),
    onNavClick: () -> Unit,
) {
    val preferences by preferencesViewModel.userPreferences.collectAsStateWithLifecycle()
    val localContext = LocalContext.current

    val directoryPickerSheetState = rememberAppSheetState()

    WidgetDefaultsScreen(
        userPreferences = preferences,
        onNavClick = onNavClick,
        onAspectRatioClick = {
            PhotoWidgetAspectRatioPicker.show(
                context = localContext,
                onAspectRatioSelected = preferencesViewModel::saveDefaultAspectRatio,
            )
        },
        onSourceClick = {
            SelectionDialog.show(
                context = localContext,
                title = localContext.getString(R.string.widget_defaults_source),
                options = PhotoWidgetSource.entries,
                optionName = { option -> localContext.getString(option.label) },
                onOptionSelected = preferencesViewModel::saveDefaultSource,
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
        onSaturationClick = {
            PhotoWidgetSaturationPicker.show(
                context = localContext,
                currentSaturation = preferences.defaultSaturation,
                onApplyClick = preferencesViewModel::saveDefaultSaturation,
            )
        },
        onBrightnessClick = {
            PhotoWidgetBrightnessPicker.show(
                context = localContext,
                currentBrightness = preferences.defaultBrightness,
                onApplyClick = preferencesViewModel::saveDefaultBrightness,
            )
        },
        onIntervalClick = {
            PhotoWidgetCycleModePicker.show(
                context = localContext,
                cycleMode = preferences.defaultCycleMode,
                onApplyClick = preferencesViewModel::saveDefaultCycleMode,
            )
        },
        onShuffleChange = preferencesViewModel::saveDefaultShuffle,
        onSortClick = directoryPickerSheetState::showBottomSheet,
        onClearDefaultsClick = preferencesViewModel::clearDefaults,
    )

    DirectorySortingBottomSheet(
        sheetState = directoryPickerSheetState,
        onItemClick = preferencesViewModel::saveDefaultSorting,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WidgetDefaultsScreen(
    userPreferences: UserPreferences,
    onNavClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onSourceClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
    onIntervalClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
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
                    IconButton(
                        onClick = onNavClick,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerDefault(
                title = stringResource(id = R.string.photo_widget_aspect_ratio_title),
                currentValue = stringResource(id = userPreferences.defaultAspectRatio.label),
                onClick = onAspectRatioClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_source),
                currentValue = stringResource(id = userPreferences.defaultSource.label),
                onClick = onSourceClick,
            )

            ShapeDefault(
                title = stringResource(id = R.string.widget_defaults_shape),
                currentValue = userPreferences.defaultShape,
                onClick = onShapeClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_corner_radius),
                currentValue = userPreferences.defaultCornerRadius.toString(),
                onClick = onCornerRadiusClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_opacity),
                currentValue = formatPercent(value = userPreferences.defaultOpacity, fractionDigits = 0),
                onClick = onOpacityClick,
            )

            PickerDefault(
                title = stringResource(R.string.widget_defaults_saturation),
                currentValue = formatRangeValue(
                    value = PhotoWidgetColors.pickerSaturation(userPreferences.defaultSaturation),
                ),
                onClick = onSaturationClick,
            )

            PickerDefault(
                title = stringResource(R.string.widget_defaults_brightness),
                currentValue = formatRangeValue(value = userPreferences.defaultBrightness),
                onClick = onBrightnessClick,
            )

            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_cycling),
                currentValue = when (userPreferences.defaultCycleMode) {
                    is PhotoWidgetCycleMode.Interval -> {
                        val intervalString = pluralStringResource(
                            id = when (userPreferences.defaultCycleMode.loopingInterval.timeUnit) {
                                TimeUnit.SECONDS -> R.plurals.photo_widget_configure_interval_current_seconds
                                TimeUnit.MINUTES -> R.plurals.photo_widget_configure_interval_current_minutes
                                TimeUnit.HOURS -> R.plurals.photo_widget_configure_interval_current_hours
                                else -> R.plurals.photo_widget_configure_interval_current_days
                            },
                            count = userPreferences.defaultCycleMode.loopingInterval.repeatInterval.toInt(),
                            userPreferences.defaultCycleMode.loopingInterval.repeatInterval,
                        )
                        stringResource(id = R.string.photo_widget_configure_interval_current_label, intervalString)
                    }

                    is PhotoWidgetCycleMode.Schedule -> {
                        pluralStringResource(
                            id = R.plurals.photo_widget_configure_schedule_times,
                            count = userPreferences.defaultCycleMode.triggers.size,
                            userPreferences.defaultCycleMode.triggers.size,
                        )
                    }

                    is PhotoWidgetCycleMode.Disabled -> {
                        stringResource(id = R.string.photo_widget_configure_cycling_mode_disabled)
                    }
                },
                onClick = onIntervalClick,
            )

            BooleanDefault(
                title = stringResource(id = R.string.widget_defaults_shuffle),
                currentValue = userPreferences.defaultShuffle,
                onCheckedChange = onShuffleChange,
            )

            PickerDefault(
                title = stringResource(R.string.photo_widget_directory_sort_title),
                currentValue = stringResource(id = userPreferences.defaultDirectorySorting.label),
                onClick = onSortClick,
            )

            OutlinedButton(
                onClick = onClearDefaultsClick,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(text = stringResource(id = R.string.widget_defaults_reset))
            }

            HorizontalDivider(modifier = Modifier.padding(all = 8.dp))

            Text(
                text = stringResource(id = R.string.widget_defaults_explanation),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// region Items
@Composable
fun BooleanDefault(
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
            AutoSizeText(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.width(24.dp))

            Switch(
                checked = currentValue,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    val icon = painterResource(if (currentValue) R.drawable.ic_check else R.drawable.ic_xmark)

                    AnimatedContent(
                        targetState = icon,
                        transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                    ) { painter ->
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
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
    warning: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors().run {
            copy(containerColor = containerColor.copy(alpha = 0.6f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AutoSizeText(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )

                Spacer(modifier = Modifier.width(24.dp))

                AutoSizeText(
                    text = currentValue,
                    modifier = Modifier.width(160.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                )
            }

            if (warning != null) {
                Text(
                    text = warning,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
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
            AutoSizeText(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.width(24.dp))

            ColoredShape(
                shapeId = currentValue,
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
        val state = rememberLazyGridState()
        val shapeSize = 80.dp
        val spacing = 8.dp
        val rowCount = 4

        LazyHorizontalGrid(
            rows = GridCells.Fixed(count = rowCount),
            modifier = Modifier
                .height(height = (shapeSize * rowCount) + (spacing * (rowCount - 1)))
                .fillMaxWidth(),
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(PhotoWidgetShapeBuilder.shapes) { shape ->
                val color by animateColorAsState(
                    targetValue = if (shape.id == selectedShapeId || selectedShapeId == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "ShapePicker_SelectedColor",
                )
                ColoredShape(
                    shapeId = shape.id,
                    color = color,
                    modifier = Modifier
                        .size(shapeSize)
                        .clickable { onClick(shape.id) },
                )
            }
        }

        LaunchedEffect(Unit) {
            val selectedIndex = PhotoWidgetShapeBuilder.shapes.indexOfFirst { it.id == selectedShapeId }
            val visibleCount = state.layoutInfo.visibleItemsInfo.size

            if (selectedIndex > visibleCount) {
                state.scrollToItem(index = selectedIndex)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CornerRadiusPicker(
    currentValue: Int,
    onApplyClick: (newValue: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.widget_defaults_corner_radius),
        modifier = modifier,
    ) {
        var value by remember(currentValue) { mutableIntStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = value.dpToPx())
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
                value = value.toFloat(),
                onValueChange = { value = it.roundToInt() },
                modifier = Modifier.weight(1f),
                valueRange = 0f..128f,
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = "$value",
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Button(
            onClick = { onApplyClick(value) },
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
@OptIn(ExperimentalMaterial3Api::class)
fun OpacityPicker(
    currentValue: Float,
    onApplyClick: (newValue: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.widget_defaults_opacity),
        modifier = modifier,
    ) {
        var value by remember(currentValue) { mutableFloatStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx(),
                    colors = PhotoWidgetColors(opacity = value),
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
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = formatPercent(value = value, fractionDigits = 0),
                modifier = Modifier.width(48.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Button(
            onClick = { onApplyClick(value) },
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
fun DefaultPicker(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState())
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
@AllPreviews
private fun WidgetDefaultsScreenPreview() {
    ExtendedTheme {
        WidgetDefaultsScreen(
            userPreferences = UserPreferences(
                dataSaver = true,
                appearance = Appearance.FOLLOW_SYSTEM,
                useTrueBlack = false,
                dynamicColors = true,
                defaultAspectRatio = PhotoWidgetAspectRatio.SQUARE,
                defaultSource = PhotoWidgetSource.PHOTOS,
                defaultShuffle = false,
                defaultDirectorySorting = DirectorySorting.NEWEST_FIRST,
                defaultCycleMode = PhotoWidgetCycleMode.DEFAULT,
                defaultShape = PhotoWidget.DEFAULT_SHAPE_ID,
                defaultCornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                defaultOpacity = PhotoWidget.DEFAULT_OPACITY,
                defaultSaturation = PhotoWidget.DEFAULT_SATURATION,
                defaultBrightness = PhotoWidget.DEFAULT_BRIGHTNESS,
            ),
            onNavClick = {},
            onAspectRatioClick = {},
            onSourceClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onIntervalClick = {},
            onShuffleChange = {},
            onSortClick = {},
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
