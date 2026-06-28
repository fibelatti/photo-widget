package com.fibelatti.photowidget.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.ui.BooleanListItem
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.SelectionDialogBottomSheet

@Composable
fun AppAppearanceBottomSheet(
    sheetState: AppSheetState,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val localResources = LocalResources.current

    val userPreferencesStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).userPreferencesStorage()
    }

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        title = stringResource(R.string.photo_widget_home_appearance),
        options = Appearance.entries,
        optionName = { appearance ->
            localResources.getString(
                when (appearance) {
                    Appearance.FOLLOW_SYSTEM -> R.string.preferences_appearance_follow_system
                    Appearance.LIGHT -> R.string.preferences_appearance_light
                    Appearance.DARK -> R.string.preferences_appearance_dark
                },
            )
        },
        onOptionSelect = { newAppearance ->
            userPreferencesStorage.appearance = newAppearance

            val mode = when (newAppearance) {
                Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            sheetState.hideBottomSheet(onHidden = { AppCompatDelegate.setDefaultNightMode(mode) })
        },
        modifier = modifier,
        footer = {
            BooleanListItem(
                headlineText = stringResource(R.string.photo_widget_home_true_black_background),
                currentValue = userPreferencesStorage.useTrueBlack,
                onValueChange = { newValue -> userPreferencesStorage.useTrueBlack = newValue },
                modifier = Modifier.padding(all = 16.dp),
            )
        },
    )
}
