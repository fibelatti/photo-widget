package com.fibelatti.photowidget.home

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun HomeScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    currentWidgets: List<Pair<Int, PhotoWidget>>,
    onCurrentWidgetClick: (appWidgetId: Int) -> Unit,
    onRemovedWidgetClick: (appWidgetId: Int) -> Unit,
    onDefaultsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onHelpClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentDestination: HomeNavigationDestination by rememberSaveable {
        mutableStateOf(HomeNavigationDestination.NEW_WIDGET)
    }

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
                (fadeIn(animationSpec = tween(220)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(220)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            contentAlignment = Alignment.Center,
            label = "Home_Navigation",
        ) { destination ->
            when (destination) {
                HomeNavigationDestination.NEW_WIDGET -> {
                    NewWidgetScreen(
                        onCreateNewWidgetClick = onCreateNewWidgetClick,
                        onHelpClick = onHelpClick,
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
                        onDataSaverClick = onDataSaverClick,
                        onAppearanceClick = onAppearanceClick,
                        onColorsClick = onColorsClick,
                        onSendFeedbackClick = onSendFeedbackClick,
                        onRateClick = onRateClick,
                        onShareClick = onShareClick,
                        onHelpClick = onHelpClick,
                        onPrivacyPolicyClick = onPrivacyPolicyClick,
                        onViewLicensesClick = onViewLicensesClick,
                    )
                }
            }
        }
    }
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
                            tint = Color.Unspecified,
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
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun HomeScreenPreview() {
    ExtendedTheme {
        HomeScreen(
            onCreateNewWidgetClick = {},
            currentWidgets = emptyList(),
            onCurrentWidgetClick = {},
            onRemovedWidgetClick = {},
            onDefaultsClick = {},
            onDataSaverClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onSendFeedbackClick = {},
            onRateClick = {},
            onShareClick = {},
            onHelpClick = {},
            onPrivacyPolicyClick = {},
            onViewLicensesClick = {},
        )
    }
}
// endregion Previews
