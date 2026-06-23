package com.fibelatti.photowidget.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.preview.PreviewLocales
import com.fibelatti.ui.preview.PreviewThemesAndColors
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.launch

@Composable
fun KeepAliveServiceBottomSheet(
    sheetState: AppSheetState,
) {
    val localContext = LocalContext.current
    val userPreferencesStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).userPreferencesStorage()
    }
    val photoWidgetStorage = remember(localContext) {
        entryPoint<PhotoWidgetEntryPoint>(localContext).photoWidgetStorage()
    }
    val coroutineScope = rememberCoroutineScope()
    var showGifWarningDialog by rememberSaveable { mutableStateOf(false) }

    if (showGifWarningDialog) {
        GifWidgetsWarningDialog(
            onDismiss = { showGifWarningDialog = false },
            onConfirm = {
                showGifWarningDialog = false
                userPreferencesStorage.keepAlive = false
                KeepAliveService.stop(localContext)
            },
        )
    }

    AppBottomSheet(
        sheetState = sheetState,
    ) {
        val preferences: UserPreferences by userPreferencesStorage.userPreferences.collectAsStateWithLifecycle()

        KeepAliveServiceContent(
            keepAlive = preferences.keepAlive,
            onKeepAliveChange = { newValue: Boolean ->
                if (newValue) {
                    userPreferencesStorage.keepAlive = true
                    KeepAliveService.tryStart(localContext)
                } else {
                    coroutineScope.launch {
                        if (photoWidgetStorage.hasActiveGifWidgets()) {
                            showGifWarningDialog = true
                        } else {
                            userPreferencesStorage.keepAlive = false
                            KeepAliveService.stop(localContext)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun GifWidgetsWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_action_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_action_cancel))
            }
        },
        text = {
            Text(text = stringResource(id = R.string.photo_widget_keep_alive_service_gif_widgets_warning))
        },
    )
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
@PreviewThemesAndColors
@PreviewLocales
private fun KeepAliveServiceContentPreview() {
    ExtendedTheme {
        KeepAliveServiceContent(
            keepAlive = true,
            onKeepAliveChange = {},
        )
    }
}
