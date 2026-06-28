package com.fibelatti.photowidget.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.CornerRadiusPicker
import com.fibelatti.photowidget.configure.DirectorySortingBottomSheet
import com.fibelatti.photowidget.configure.OpacityPicker
import com.fibelatti.photowidget.configure.PhotoWidgetAspectRatioBottomSheet
import com.fibelatti.photowidget.configure.PhotoWidgetBrightnessBottomSheet
import com.fibelatti.photowidget.configure.PhotoWidgetCycleModeBottomSheet
import com.fibelatti.photowidget.configure.PhotoWidgetSaturationBottomSheet
import com.fibelatti.photowidget.configure.ShapePicker
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.ui.BooleanListItem
import com.fibelatti.photowidget.ui.PickerListItem
import com.fibelatti.photowidget.ui.ShapeListItem
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Back
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.SelectionDialogBottomSheet
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit

@Composable
fun WidgetDefaultsScreen(
    preferencesViewModel: WidgetDefaultsViewModel = hiltViewModel(),
    onNavClick: () -> Unit,
) {
    val preferences by preferencesViewModel.userPreferences.collectAsStateWithLifecycle()

    val aspectRatioPickerSheetState = rememberAppSheetState()
    val sourcePickerSheetState = rememberAppSheetState()
    val shapePickerSheetState = rememberAppSheetState()
    val cornerRadiusPickerSheetState = rememberAppSheetState()
    val opacityPickerSheetState = rememberAppSheetState()
    val saturationPickerSheetState = rememberAppSheetState()
    val brightnessPickerSheetState = rememberAppSheetState()
    val cycleModePickerSheetState = rememberAppSheetState()
    val directoryPickerSheetState = rememberAppSheetState()

    val localResources = LocalResources.current

    WidgetDefaultsScreen(
        userPreferences = preferences,
        onNavClick = onNavClick,
        onAspectRatioClick = aspectRatioPickerSheetState::showBottomSheet,
        onSourceClick = sourcePickerSheetState::showBottomSheet,
        onShapeClick = shapePickerSheetState::showBottomSheet,
        onCornerRadiusClick = cornerRadiusPickerSheetState::showBottomSheet,
        onOpacityClick = opacityPickerSheetState::showBottomSheet,
        onSaturationClick = saturationPickerSheetState::showBottomSheet,
        onBrightnessClick = brightnessPickerSheetState::showBottomSheet,
        onIntervalClick = cycleModePickerSheetState::showBottomSheet,
        onShuffleChange = preferencesViewModel::saveDefaultShuffle,
        onSortClick = directoryPickerSheetState::showBottomSheet,
        onClearDefaultsClick = preferencesViewModel::clearDefaults,
    )

    // region Bottom Sheets
    PhotoWidgetAspectRatioBottomSheet(
        sheetState = aspectRatioPickerSheetState,
        onAspectRatioSelect = preferencesViewModel::saveDefaultAspectRatio,
    )

    SelectionDialogBottomSheet(
        sheetState = sourcePickerSheetState,
        title = stringResource(R.string.widget_defaults_source),
        options = PhotoWidgetSource.entries,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelect = preferencesViewModel::saveDefaultSource,
    )

    AppBottomSheet(
        sheetState = shapePickerSheetState,
    ) {
        ShapePicker(
            onClick = { newShapeId ->
                preferencesViewModel.saveDefaultShape(newShapeId)
                shapePickerSheetState.hideBottomSheet()
            },
        )
    }

    AppBottomSheet(
        sheetState = cornerRadiusPickerSheetState,
    ) {
        CornerRadiusPicker(
            currentValue = preferences.defaultCornerRadius,
            onApplyClick = { newValue ->
                preferencesViewModel.saveDefaultCornerRadius(newValue)
                cornerRadiusPickerSheetState.hideBottomSheet()
            },
        )
    }

    AppBottomSheet(
        sheetState = opacityPickerSheetState,
    ) {
        OpacityPicker(
            currentValue = preferences.defaultOpacity,
            onApplyClick = { newValue ->
                preferencesViewModel.saveDefaultOpacity(newValue)
                opacityPickerSheetState.hideBottomSheet()
            },
        )
    }

    PhotoWidgetSaturationBottomSheet(
        sheetState = saturationPickerSheetState,
        currentSaturation = preferences.defaultSaturation,
        onApplyClick = preferencesViewModel::saveDefaultSaturation,
    )

    PhotoWidgetBrightnessBottomSheet(
        sheetState = brightnessPickerSheetState,
        currentBrightness = preferences.defaultBrightness,
        onApplyClick = preferencesViewModel::saveDefaultBrightness,
    )

    PhotoWidgetCycleModeBottomSheet(
        sheetState = cycleModePickerSheetState,
        cycleMode = preferences.defaultCycleMode,
        canUseAdvancedSchedule = true,
        onApplyClick = preferencesViewModel::saveDefaultCycleMode,
    )

    DirectorySortingBottomSheet(
        sheetState = directoryPickerSheetState,
        onItemClick = preferencesViewModel::saveDefaultSorting,
    )
    // endregion Bottom Sheets
}

@Composable
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
        modifier = modifier,
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
                            imageVector = AppIcons.Back,
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
        WidgetDefaultsContent(
            userPreferences = userPreferences,
            onAspectRatioClick = onAspectRatioClick,
            onSourceClick = onSourceClick,
            onShapeClick = onShapeClick,
            onCornerRadiusClick = onCornerRadiusClick,
            onOpacityClick = onOpacityClick,
            onSaturationClick = onSaturationClick,
            onBrightnessClick = onBrightnessClick,
            onIntervalClick = onIntervalClick,
            onShuffleChange = onShuffleChange,
            onSortClick = onSortClick,
            onClearDefaultsClick = onClearDefaultsClick,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        )
    }
}

@Composable
private fun WidgetDefaultsContent(
    userPreferences: UserPreferences,
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PickerListItem(
            headlineText = stringResource(id = R.string.photo_widget_aspect_ratio_title),
            currentValue = stringResource(id = userPreferences.defaultAspectRatio.label),
            onClick = onAspectRatioClick,
            shape = Shapes.TopShape,
        )

        PickerListItem(
            headlineText = stringResource(id = R.string.widget_defaults_source),
            currentValue = stringResource(id = userPreferences.defaultSource.label),
            onClick = onSourceClick,
            shape = Shapes.MiddleShape,
        )

        ShapeListItem(
            headlineText = stringResource(id = R.string.widget_defaults_shape),
            currentValue = userPreferences.defaultShape,
            onClick = onShapeClick,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(id = R.string.widget_defaults_corner_radius),
            currentValue = userPreferences.defaultCornerRadius.toString(),
            onClick = onCornerRadiusClick,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(id = R.string.widget_defaults_opacity),
            currentValue = formatPercent(value = userPreferences.defaultOpacity, fractionDigits = 0),
            onClick = onOpacityClick,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(R.string.widget_defaults_saturation),
            currentValue = formatRangeValue(
                value = PhotoWidgetColors.pickerSaturation(userPreferences.defaultSaturation),
            ),
            onClick = onSaturationClick,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(R.string.widget_defaults_brightness),
            currentValue = formatRangeValue(value = userPreferences.defaultBrightness),
            onClick = onBrightnessClick,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(id = R.string.widget_defaults_cycling),
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

                is PhotoWidgetCycleMode.AdvancedSchedule -> {
                    stringResource(id = R.string.photo_widget_configure_cycle_mode_advanced_schedule)
                }

                is PhotoWidgetCycleMode.Disabled -> {
                    stringResource(id = R.string.photo_widget_configure_cycling_mode_disabled)
                }
            },
            onClick = onIntervalClick,
            shape = Shapes.MiddleShape,
        )

        BooleanListItem(
            headlineText = stringResource(id = R.string.widget_defaults_shuffle),
            currentValue = userPreferences.defaultShuffle,
            onValueChange = onShuffleChange,
            shape = Shapes.MiddleShape,
        )

        PickerListItem(
            headlineText = stringResource(R.string.photo_widget_directory_sort_title),
            currentValue = stringResource(id = userPreferences.defaultDirectorySorting.label),
            onClick = onSortClick,
            shape = Shapes.BottomShape,
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

// region Previews
@Composable
@PreviewAll
private fun WidgetDefaultsScreenPreview() {
    ExtendedTheme {
        WidgetDefaultsScreen(
            userPreferences = UserPreferences(
                dataSaver = true,
                keepAlive = true,
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
                highlightTransparentWidgets = false,
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
// endregion Previews
