package com.fibelatti.photowidget.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.ui.BooleanListItem
import com.fibelatti.photowidget.ui.PickerListItem
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Back
import com.fibelatti.photowidget.widget.PhotoWidgetSyncWorker
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun WidgetSettingsScreen(
    widgetSettingsViewModel: WidgetSettingsViewModel = hiltViewModel(),
    onNavClick: () -> Unit,
) {
    val preferences by widgetSettingsViewModel.userPreferences.collectAsStateWithLifecycle()

    val folderSyncIntervalSheetState = rememberAppSheetState()

    WidgetSettingsScreen(
        userPreferences = preferences,
        onNavClick = onNavClick,
        onEnableCrossfadeChange = widgetSettingsViewModel::saveEnableCrossfade,
        onFolderSyncIntervalClick = folderSyncIntervalSheetState::showBottomSheet,
    )

    FolderSyncIntervalBottomSheet(
        sheetState = folderSyncIntervalSheetState,
        currentValue = preferences.folderSyncInterval,
        onApplyClick = widgetSettingsViewModel::saveFolderSyncInterval,
    )
}

@Composable
private fun WidgetSettingsScreen(
    userPreferences: UserPreferences,
    onNavClick: () -> Unit,
    onEnableCrossfadeChange: (Boolean) -> Unit,
    onFolderSyncIntervalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.widget_settings_title))
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
        WidgetSettingsContent(
            userPreferences = userPreferences,
            onEnableCrossfadeChange = onEnableCrossfadeChange,
            onFolderSyncIntervalClick = onFolderSyncIntervalClick,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        )
    }
}

@Composable
private fun WidgetSettingsContent(
    userPreferences: UserPreferences,
    onEnableCrossfadeChange: (Boolean) -> Unit,
    onFolderSyncIntervalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BooleanListItem(
            headlineText = stringResource(id = R.string.widget_settings_crossfade),
            currentValue = userPreferences.widgetEnableCrossfade,
            onValueChange = onEnableCrossfadeChange,
            supportingText = stringResource(id = R.string.widget_settings_crossfade_description),
            shape = Shapes.TopShape,
            headlineFlag = {
                Text(
                    text = stringResource(R.string.warning_experimental),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )

        PickerListItem(
            headlineText = stringResource(id = R.string.widget_settings_folder_sync_interval),
            currentValue = folderSyncIntervalLabel(value = userPreferences.folderSyncInterval),
            onClick = onFolderSyncIntervalClick,
            shape = Shapes.BottomShape,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Text(
            text = stringResource(id = R.string.widget_settings_explanation),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// region Previews
@Composable
@PreviewAll
private fun WidgetSettingsScreenPreview() {
    ExtendedTheme {
        WidgetSettingsScreen(
            userPreferences = UserPreferences(
                dataSaver = true,
                keepAlive = true,
                appearance = Appearance.FOLLOW_SYSTEM,
                useTrueBlack = false,
                dynamicColors = true,
                widgetEnableCrossfade = false,
                folderSyncInterval = PhotoWidgetSyncWorker.DEFAULT_INTERVAL_HOURS,
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
            onEnableCrossfadeChange = {},
            onFolderSyncIntervalClick = {},
        )
    }
}
// endregion Previews
