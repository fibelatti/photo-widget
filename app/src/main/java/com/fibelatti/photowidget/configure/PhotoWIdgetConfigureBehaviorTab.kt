package com.fibelatti.photowidget.configure

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.canShuffle
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import com.fibelatti.photowidget.preferences.BooleanDefault
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit

@Composable
fun PhotoWidgetConfigureBehaviorTab(
    viewModel: PhotoWidgetConfigureViewModel,
    onNav: (PhotoWidgetConfigureNav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    var showBackgroundRestrictionDialog: Boolean by remember { mutableStateOf(false) }

    val backgroundRestrictionSheetState: AppSheetState = rememberAppSheetState()
    val cycleModePickerSheetState: AppSheetState = rememberAppSheetState()
    val directoryPickerSheetState: AppSheetState = rememberAppSheetState()

    val localContext: Context = LocalContext.current

    PhotoWidgetConfigureBehaviorTab(
        photoWidget = state.photoWidget,
        onCycleModePickerClick = {
            if (localContext.isBackgroundRestricted(checkUnrestrictedBattery = true)) {
                showBackgroundRestrictionDialog = true
            } else {
                cycleModePickerSheetState.showBottomSheet()
            }
        },
        onShuffleChange = viewModel::saveShuffle,
        onSortClick = directoryPickerSheetState::showBottomSheet,
        onTapActionPickerClick = { onNav(PhotoWidgetConfigureNav.TapActionPicker) },
        modifier = modifier,
    )

    if (showBackgroundRestrictionDialog) {
        BackgroundRestrictionWarningDialog(
            onLearnMoreClick = {
                showBackgroundRestrictionDialog = false
                backgroundRestrictionSheetState.showBottomSheet()
            },
            onIgnoreClick = {
                showBackgroundRestrictionDialog = false
                cycleModePickerSheetState.showBottomSheet()
            },
        )
    }

    BackgroundRestrictionBottomSheet(
        sheetState = backgroundRestrictionSheetState,
    )

    PhotoWidgetCycleModeBottomSheet(
        sheetState = cycleModePickerSheetState,
        cycleMode = state.photoWidget.cycleMode,
        onApplyClick = viewModel::cycleModeSelected,
    )

    DirectorySortingBottomSheet(
        sheetState = directoryPickerSheetState,
        onItemClick = viewModel::saveSorting,
    )
}

@Composable
fun PhotoWidgetConfigureBehaviorTab(
    photoWidget: PhotoWidget,
    onCycleModePickerClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
    onTapActionPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        if (photoWidget.photos.size > 1) {
            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_cycling),
                currentValue = when (photoWidget.cycleMode) {
                    is PhotoWidgetCycleMode.Interval -> {
                        val intervalString = pluralStringResource(
                            id = when (photoWidget.cycleMode.loopingInterval.timeUnit) {
                                TimeUnit.SECONDS -> R.plurals.photo_widget_configure_interval_current_seconds
                                TimeUnit.MINUTES -> R.plurals.photo_widget_configure_interval_current_minutes
                                TimeUnit.HOURS -> R.plurals.photo_widget_configure_interval_current_hours
                                else -> R.plurals.photo_widget_configure_interval_current_days
                            },
                            count = photoWidget.cycleMode.loopingInterval.repeatInterval.toInt(),
                            photoWidget.cycleMode.loopingInterval.repeatInterval,
                        )
                        stringResource(id = R.string.photo_widget_configure_interval_current_label, intervalString)
                    }

                    is PhotoWidgetCycleMode.Schedule -> {
                        pluralStringResource(
                            id = R.plurals.photo_widget_configure_schedule_times,
                            count = photoWidget.cycleMode.triggers.size,
                            photoWidget.cycleMode.triggers.size,
                        )
                    }

                    is PhotoWidgetCycleMode.Disabled -> {
                        stringResource(id = R.string.photo_widget_configure_cycling_mode_disabled)
                    }
                },
                onClick = onCycleModePickerClick,
            )
        }

        if (photoWidget.canShuffle) {
            Spacer(modifier = Modifier.height(12.dp))

            BooleanDefault(
                title = stringResource(R.string.widget_defaults_shuffle),
                currentValue = photoWidget.shuffle,
                onCheckedChange = onShuffleChange,
            )
        }

        AnimatedVisibility(
            visible = PhotoWidgetSource.DIRECTORY == photoWidget.source && !photoWidget.shuffle,
        ) {
            PickerDefault(
                title = stringResource(R.string.photo_widget_directory_sort_title),
                currentValue = stringResource(id = photoWidget.directorySorting.label),
                onClick = onSortClick,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_tap_action),
            currentValue = buildString {
                appendLine(stringResource(id = photoWidget.tapActions.left.label))
                appendLine(stringResource(id = photoWidget.tapActions.center.label))
                appendLine(stringResource(id = photoWidget.tapActions.right.label))
            },
            onClick = onTapActionPickerClick,
        )
    }
}

// region Previews
@AllPreviews
@Composable
private fun PhotoWidgetConfigureBehaviorTabSinglePhotoPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureBehaviorTab(
            photoWidget = PhotoWidget(
                photos = List(1) { index -> LocalPhoto(photoId = "photo-$index") },
            ),
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onSortClick = {},
            onTapActionPickerClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@AllPreviews
@Composable
private fun PhotoWidgetConfigureBehaviorTabMultiPhotoPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureBehaviorTab(
            photoWidget = PhotoWidget(
                photos = List(10) { index -> LocalPhoto(photoId = "photo-$index") },
                source = PhotoWidgetSource.DIRECTORY,
            ),
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onSortClick = {},
            onTapActionPickerClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
// endregion Previews
