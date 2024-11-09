package com.fibelatti.photowidget.preferences

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.ui.Toggle
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

object DataSaverPicker {

    fun show(context: Context) {
        val userPreferencesStorage = entryPoint<PhotoWidgetEntryPoint>(context).userPreferencesStorage()

        ComposeBottomSheetDialog(context) {
            val preferences by userPreferencesStorage.userPreferences.collectAsStateWithLifecycle()

            DataSaverPickerContent(
                dataSaver = preferences.dataSaver,
                onDataSaverChange = { newValue -> userPreferencesStorage.dataSaver = newValue },
            )
        }.show()
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

        Toggle(
            title = stringResource(R.string.preferences_data_saver),
            checked = dataSaver,
            onCheckedChange = onDataSaverChange,
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
@ThemePreviews
@LocalePreviews
private fun DataSaverPickerContentPreview() {
    ExtendedTheme {
        DataSaverPickerContent(
            dataSaver = true,
            onDataSaverChange = {},
        )
    }
}
