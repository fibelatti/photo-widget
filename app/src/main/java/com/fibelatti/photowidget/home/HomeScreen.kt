package com.fibelatti.photowidget.home

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.backup.PhotoWidgetBackupActivity
import com.fibelatti.photowidget.configure.BackgroundRestrictionBottomSheet
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.help.HelpBottomSheet
import com.fibelatti.photowidget.licenses.OssLicensesActivity
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.preferences.AppAppearanceBottomSheet
import com.fibelatti.photowidget.preferences.AppColorsBottomSheet
import com.fibelatti.photowidget.preferences.DataSaverBottomSheet
import com.fibelatti.photowidget.preferences.WidgetDefaultsActivity
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    preparedIntent: Intent?,
    onIntentConsumed: () -> Unit,
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onAppLanguageClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val currentWidgets by homeViewModel.currentWidgets.collectAsStateWithLifecycle()

    val existingWidgetMenuSheetState = rememberAppSheetState()
    val removedWidgetSheetState = rememberAppSheetState()
    val appAppearanceSheetState = rememberAppSheetState()
    val appColorsSheetState = rememberAppSheetState()

    val localContext = LocalContext.current
    val localUriHandler = LocalUriHandler.current

    val hintStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).hintStorage()
    }
    var showBackgroundRestrictionHint by remember {
        mutableStateOf(hintStorage.showHomeBackgroundRestrictionsHint)
    }

    HomeScreen(
        onCreateNewWidgetClick = onCreateNewWidgetClick,
        currentWidgets = currentWidgets,
        onCurrentWidgetClick = click@{ appWidgetId: Int, canSync: Boolean, canLock: Boolean, isLocked: Boolean ->
            preparedIntent?.let { intent ->
                intent.appWidgetId = appWidgetId

                onIntentConsumed()

                localContext.startActivity(intent)

                return@click
            }

            existingWidgetMenuSheetState.showBottomSheet(
                data = ExistingWidgetMenuBottomSheetData(
                    appWidgetId = appWidgetId,
                    canSync = canSync,
                    canLock = canLock,
                    isLocked = isLocked,
                ),
            )
        },
        onRemovedWidgetClick = { appWidgetId, photoWidgetStatus ->
            removedWidgetSheetState.showBottomSheet(
                data = RemovedWidgetBottomSheetData(
                    appWidgetId = appWidgetId,
                    status = photoWidgetStatus,
                ),
            )
        },
        onDefaultsClick = {
            localContext.startActivity(Intent(localContext, WidgetDefaultsActivity::class.java))
        },
        onAppearanceClick = appAppearanceSheetState::showBottomSheet,
        onColorsClick = appColorsSheetState::showBottomSheet,
        onAppLanguageClick = onAppLanguageClick,
        onBackupClick = {
            localContext.startActivity(PhotoWidgetBackupActivity.newIntent(localContext))
        },
        onRateClick = {
            localUriHandler.openUri("https://play.google.com/store/apps/details?id=com.fibelatti.photowidget")
        },
        onShareClick = onShareClick,
        showBackgroundRestrictionHint = showBackgroundRestrictionHint,
        onDismissWarningClick = {
            hintStorage.showHomeBackgroundRestrictionsHint = false
            showBackgroundRestrictionHint = false
        },
        onPrivacyPolicyClick = {
            localUriHandler.openUri("https://www.fibelatti.com/privacy-policy/material-photo-widget")
        },
        onViewLicensesClick = {
            localContext.startActivity(Intent(localContext, OssLicensesActivity::class.java))
        },
    )

    // region Bottom sheets
    ExistingWidgetMenuBottomSheet(
        sheetState = existingWidgetMenuSheetState,
        onSync = homeViewModel::syncPhotos,
        onLock = homeViewModel::lockWidget,
        onUnlock = homeViewModel::unlockWidget,
    )

    RemovedWidgetBottomSheet(
        sheetState = removedWidgetSheetState,
        onKeep = homeViewModel::keepWidget,
        onDelete = homeViewModel::deleteWidget,
    )

    AppAppearanceBottomSheet(
        sheetState = appAppearanceSheetState,
    )

    AppColorsBottomSheet(
        sheetState = appColorsSheetState,
    )
    // endregion Bottom sheets
}

@Composable
fun HomeScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    currentWidgets: List<Pair<Int, PhotoWidget>>,
    onCurrentWidgetClick: (appWidgetId: Int, canSync: Boolean, canLock: Boolean, isLocked: Boolean) -> Unit,
    onRemovedWidgetClick: (appWidgetId: Int, PhotoWidgetStatus) -> Unit,
    onDefaultsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    showBackgroundRestrictionHint: Boolean,
    onDismissWarningClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentDestination: HomeNavigationDestination by rememberSaveable {
        mutableStateOf(HomeNavigationDestination.NEW_WIDGET)
    }

    val helpSheetState = rememberAppSheetState()
    val backgroundRestrictionSheetState = rememberAppSheetState()
    val dataSaverSheetState = rememberAppSheetState()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            HomeNavigation(
                currentDestination = currentDestination,
                onDestinationClick = { currentDestination = it },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentDestination,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues),
            transitionSpec = {
                fadeIn(animationSpec = tween(220))
                    .plus(scaleIn(initialScale = 0.97f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            contentAlignment = Alignment.Center,
            label = "Home_Navigation",
        ) { destination ->
            when (destination) {
                HomeNavigationDestination.NEW_WIDGET -> {
                    NewWidgetScreen(
                        onCreateNewWidgetClick = onCreateNewWidgetClick,
                        onHelpClick = helpSheetState::showBottomSheet,
                        showBackgroundRestrictionHint = showBackgroundRestrictionHint,
                        onBackgroundRestrictionClick = backgroundRestrictionSheetState::showBottomSheet,
                        onDismissWarningClick = onDismissWarningClick,
                    )
                }

                HomeNavigationDestination.MY_WIDGETS -> {
                    MyWidgetsScreen(
                        widgets = currentWidgets,
                        onCurrentWidgetClick = onCurrentWidgetClick,
                        onRemovedWidgetClick = onRemovedWidgetClick,
                    )
                }

                HomeNavigationDestination.SETTINGS -> {
                    SettingsScreen(
                        onDefaultsClick = onDefaultsClick,
                        onDataSaverClick = dataSaverSheetState::showBottomSheet,
                        onAppearanceClick = onAppearanceClick,
                        onColorsClick = onColorsClick,
                        onAppLanguageClick = onAppLanguageClick,
                        onBackupClick = onBackupClick,
                        onSendFeedbackClick = helpSheetState::showBottomSheet,
                        onRateClick = onRateClick,
                        onShareClick = onShareClick,
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onViewLicensesClick = onViewLicensesClick,
                    )
                }
            }
        }
    }

    HelpBottomSheet(
        sheetState = helpSheetState,
        onBackgroundRestrictionClick = {
            helpSheetState.hideBottomSheet()
            backgroundRestrictionSheetState.showBottomSheet()
        },
    )

    BackgroundRestrictionBottomSheet(
        sheetState = backgroundRestrictionSheetState,
    )

    DataSaverBottomSheet(
        sheetState = dataSaverSheetState,
    )
}

@Composable
private fun HomeNavigation(
    currentDestination: HomeNavigationDestination,
    onDestinationClick: (HomeNavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        HomeNavigationDestination.entries.forEach { destination ->
            val selected = destination == currentDestination

            val scale by animateFloatAsState(
                targetValue = if (selected) 1.1f else 1f,
                animationSpec = tween(),
                label = "NavIcon_Scale",
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination) },
                icon = {
                    AnimatedContent(
                        targetState = if (selected) destination.iconSelected else destination.icon,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "NavIcon",
                    ) { iconRes ->
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                label = {
                    Text(text = stringResource(id = destination.label))
                },
            )
        }
    }
}

private enum class HomeNavigationDestination(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val iconSelected: Int,
) {

    NEW_WIDGET(
        label = R.string.photo_widget_home_new,
        icon = R.drawable.ic_new_widget,
        iconSelected = R.drawable.ic_new_widget_selected,
    ),
    MY_WIDGETS(
        label = R.string.photo_widget_home_current,
        icon = R.drawable.ic_my_widgets,
        iconSelected = R.drawable.ic_my_widgets_selected,
    ),
    SETTINGS(
        label = R.string.photo_widget_home_settings,
        icon = R.drawable.ic_settings,
        iconSelected = R.drawable.ic_settings_selected,
    ),
}

// region Previews
@Composable
@AllPreviews
private fun HomeScreenPreview() {
    ExtendedTheme {
        HomeScreen(
            onCreateNewWidgetClick = {},
            currentWidgets = emptyList(),
            onCurrentWidgetClick = { _, _, _, _ -> },
            onRemovedWidgetClick = { _, _ -> },
            onDefaultsClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onAppLanguageClick = {},
            onBackupClick = {},
            onRateClick = {},
            onShareClick = {},
            showBackgroundRestrictionHint = true,
            onDismissWarningClick = {},
            onPrivacyPolicyClick = {},
            onViewLicensesClick = {},
        )
    }
}
// endregion Previews
