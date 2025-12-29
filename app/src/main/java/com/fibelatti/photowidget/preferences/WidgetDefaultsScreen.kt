@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.preferences

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
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
        onAspectRatioSelected = preferencesViewModel::saveDefaultAspectRatio,
    )

    SelectionDialogBottomSheet(
        sheetState = sourcePickerSheetState,
        title = stringResource(R.string.widget_defaults_source),
        options = PhotoWidgetSource.entries,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelected = preferencesViewModel::saveDefaultSource,
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
        onApplyClick = preferencesViewModel::saveDefaultCycleMode,
    )

    DirectorySortingBottomSheet(
        sheetState = directoryPickerSheetState,
        onItemClick = preferencesViewModel::saveDefaultSorting,
    )
    // endregion Bottom Sheets
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
            modifier = modifier
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
                    modifier = Modifier.widthIn(max = 200.dp),
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
