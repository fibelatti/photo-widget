package com.fibelatti.photowidget.home

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet

@Composable
fun AppColorsBottomSheet(
    sheetState: AppSheetState,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val localActivity = LocalActivity.current

    val userPreferencesStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).userPreferencesStorage()
    }

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        title = stringResource(R.string.photo_widget_home_dynamic_colors),
        options = listOf(true, false),
        optionName = { value ->
            localContext.getString(
                if (value) {
                    R.string.preferences_dynamic_colors_enabled
                } else {
                    R.string.preferences_dynamic_colors_disabled
                },
            )
        },
        onOptionSelected = { newValue ->
            userPreferencesStorage.dynamicColors = newValue
            localActivity?.let(ActivityCompat::recreate)
        },
        modifier = modifier,
    )
}
