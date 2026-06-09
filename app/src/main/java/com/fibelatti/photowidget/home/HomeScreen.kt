package com.fibelatti.photowidget.home

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.backup.PhotoWidgetBackupScreen
import com.fibelatti.photowidget.configure.BackgroundRestrictionBottomSheet
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.help.HelpBottomSheet
import com.fibelatti.photowidget.licenses.OssLicensesScreen
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.model.canLock
import com.fibelatti.photowidget.model.canSync
import com.fibelatti.photowidget.model.isWidgetRemoved
import com.fibelatti.photowidget.platform.popNavKey
import com.fibelatti.photowidget.preferences.AppAppearanceBottomSheet
import com.fibelatti.photowidget.preferences.AppColorsBottomSheet
import com.fibelatti.photowidget.preferences.DataSaverBottomSheet
import com.fibelatti.photowidget.preferences.KeepAliveServiceBottomSheet
import com.fibelatti.photowidget.preferences.WidgetDefaultsScreen
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.MyWidgets
import com.fibelatti.photowidget.ui.icons.MyWidgetsSelected
import com.fibelatti.photowidget.ui.icons.NewWidget
import com.fibelatti.photowidget.ui.icons.NewWidgetSelected
import com.fibelatti.photowidget.ui.icons.Settings
import com.fibelatti.photowidget.ui.icons.SettingsSelected
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Suppress("ktlint:compose:vm-forwarding-check")
@Composable
fun HomeScreenNavDisplay(
    homeViewModel: HomeViewModel,
    preparedIntent: Intent?,
    onIntentConsume: () -> Unit,
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onRestoreWidgetClick: (PhotoWidget) -> Unit,
    onAppLanguageClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val navBackStack: NavBackStack<NavKey> = rememberNavBackStack(HomeNav.Home)

    NavDisplay(
        backStack = navBackStack,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        },
        popTransitionSpec = {
            EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<HomeNav.Home> {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    preparedIntent = preparedIntent,
                    onIntentConsume = onIntentConsume,
                    onCreateNewWidgetClick = onCreateNewWidgetClick,
                    onDefaultsClick = { navBackStack.add(HomeNav.WidgetDefaults) },
                    onBackupClick = { navBackStack.add(HomeNav.WidgetBackup) },
                    onAppLanguageClick = onAppLanguageClick,
                    onShareClick = onShareClick,
                    onViewLicensesClick = { navBackStack.add(HomeNav.OssLicenses) },
                )
            }

            entry<HomeNav.WidgetDefaults> {
                WidgetDefaultsScreen(
                    onNavClick = navBackStack::popNavKey,
                )
            }

            entry<HomeNav.WidgetBackup> {
                PhotoWidgetBackupScreen(
                    onNavClick = navBackStack::popNavKey,
                    onRestoreClick = onRestoreWidgetClick,
                )
            }

            entry<HomeNav.OssLicenses> {
                OssLicensesScreen(
                    onBackNavClick = navBackStack::popNavKey,
                )
            }
        },
    )
}

@Composable
private fun HomeScreen(
    homeViewModel: HomeViewModel,
    preparedIntent: Intent?,
    onIntentConsume: () -> Unit,
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onDefaultsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
) {
    val currentWidgets by homeViewModel.currentWidgets.collectAsStateWithLifecycle()

    val existingWidgetMenuSheetState = rememberAppSheetState()
    val removedWidgetSheetState = rememberAppSheetState()
    val invalidWidgetSheetState = rememberAppSheetState()
    val draftWidgetSheetState = rememberAppSheetState()
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
        onWidgetClick = { appWidgetId: Int, widget: PhotoWidget ->
            when {
                widget.status == PhotoWidgetStatus.DRAFT -> {
                    draftWidgetSheetState.showBottomSheet(data = appWidgetId)
                }

                widget.status.isWidgetRemoved -> {
                    removedWidgetSheetState.showBottomSheet(
                        data = RemovedWidgetBottomSheetData(appWidgetId = appWidgetId, status = widget.status),
                    )
                }

                widget.status == PhotoWidgetStatus.INVALID -> {
                    invalidWidgetSheetState.showBottomSheet(data = appWidgetId)
                }

                preparedIntent != null -> {
                    preparedIntent.appWidgetId = appWidgetId
                    onIntentConsume()
                    localContext.startActivity(preparedIntent)
                }

                else -> {
                    existingWidgetMenuSheetState.showBottomSheet(
                        data = ExistingWidgetMenuBottomSheetData(
                            appWidgetId = appWidgetId,
                            canSync = widget.canSync,
                            canLock = widget.canLock,
                            isLocked = widget.status == PhotoWidgetStatus.LOCKED,
                        ),
                    )
                }
            }
        },
        onDefaultsClick = onDefaultsClick,
        onAppearanceClick = appAppearanceSheetState::showBottomSheet,
        onColorsClick = appColorsSheetState::showBottomSheet,
        onAppLanguageClick = onAppLanguageClick,
        onBackupClick = onBackupClick,
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
        onViewLicensesClick = onViewLicensesClick,
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

    InvalidWidgetBottomSheet(
        sheetState = invalidWidgetSheetState,
        onDelete = homeViewModel::deleteWidget,
    )

    DraftWidgetBottomSheet(
        sheetState = draftWidgetSheetState,
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
    onWidgetClick: (id: Int, PhotoWidget) -> Unit,
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
    val keepAliveSheetState = rememberAppSheetState()

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
                        onWidgetClick = onWidgetClick,
                    )
                }

                HomeNavigationDestination.SETTINGS -> {
                    SettingsScreen(
                        onDefaultsClick = onDefaultsClick,
                        onDataSaverClick = dataSaverSheetState::showBottomSheet,
                        onKeepAliveClick = keepAliveSheetState::showBottomSheet,
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

    KeepAliveServiceBottomSheet(
        sheetState = keepAliveSheetState,
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
                    ) { vector ->
                        Icon(
                            painter = rememberVectorPainter(vector),
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
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {

    NEW_WIDGET(
        label = R.string.photo_widget_home_new,
        icon = AppIcons.NewWidget,
        iconSelected = AppIcons.NewWidgetSelected,
    ),
    MY_WIDGETS(
        label = R.string.photo_widget_home_current,
        icon = AppIcons.MyWidgets,
        iconSelected = AppIcons.MyWidgetsSelected,
    ),
    SETTINGS(
        label = R.string.photo_widget_home_settings,
        icon = AppIcons.Settings,
        iconSelected = AppIcons.SettingsSelected,
    ),
}

// region Previews
@Composable
@PreviewAll
private fun HomeScreenPreview() {
    ExtendedTheme {
        HomeScreen(
            onCreateNewWidgetClick = {},
            currentWidgets = emptyList(),
            onWidgetClick = { _, _ -> },
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
