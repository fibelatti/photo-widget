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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.ui.Toggle
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun KeepAliveServiceBottomSheet(
    sheetState: AppSheetState,
) {
    val localContext = LocalContext.current
    val userPreferencesStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).userPreferencesStorage()
    }

    AppBottomSheet(
        sheetState = sheetState,
    ) {
        val preferences: UserPreferences by userPreferencesStorage.userPreferences.collectAsStateWithLifecycle()

        KeepAliveServiceContent(
            keepAlive = preferences.keepAlive,
            onKeepAliveChange = { newValue: Boolean ->
                userPreferencesStorage.keepAlive = newValue

                if (newValue) {
                    KeepAliveService.tryStart(localContext)
                } else {
                    KeepAliveService.stop(localContext)
                }
            },
        )
    }
}

@Composable
private fun KeepAliveServiceContent(
    keepAlive: Boolean,
    onKeepAliveChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_keep_alive_service_dialog_title),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Toggle(
            title = stringResource(R.string.photo_widget_keep_alive_service_dialog_toggle),
            checked = keepAlive,
            onCheckedChange = onKeepAliveChange,
        )

        Text(
            text = AnnotatedString.fromHtml(stringResource(R.string.photo_widget_keep_alive_service_dialog_body)),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
private fun KeepAliveServiceContentPreview() {
    ExtendedTheme {
        KeepAliveServiceContent(
            keepAlive = true,
            onKeepAliveChange = {},
        )
    }
}
