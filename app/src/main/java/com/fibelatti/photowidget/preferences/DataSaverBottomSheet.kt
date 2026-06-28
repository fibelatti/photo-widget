package com.fibelatti.photowidget.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.ui.BooleanListItem
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.preview.PreviewLocales
import com.fibelatti.ui.preview.PreviewThemesAndColors
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun DataSaverBottomSheet(
    sheetState: AppSheetState,
) {
    val localContext = LocalContext.current
    val userPreferencesStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).userPreferencesStorage()
    }

    AppBottomSheet(
        sheetState = sheetState,
    ) {
        val preferences by userPreferencesStorage.userPreferences.collectAsStateWithLifecycle()

        DataSaverPickerContent(
            dataSaver = preferences.dataSaver,
            onDataSaverChange = { newValue -> userPreferencesStorage.dataSaver = newValue },
        )
    }
}

@Composable
private fun DataSaverPickerContent(
    dataSaver: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_home_data_saver),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        BooleanListItem(
            headlineText = stringResource(R.string.preferences_data_saver),
            currentValue = dataSaver,
            onValueChange = onDataSaverChange,
        )

        Text(
            text = stringResource(R.string.preferences_data_saver_description),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@PreviewThemesAndColors
@PreviewLocales
private fun DataSaverPickerContentPreview() {
    ExtendedTheme {
        DataSaverPickerContent(
            dataSaver = true,
            onDataSaverChange = {},
        )
    }
}
