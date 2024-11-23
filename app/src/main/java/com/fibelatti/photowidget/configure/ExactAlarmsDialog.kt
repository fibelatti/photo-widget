package com.fibelatti.photowidget.configure

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fibelatti.photowidget.R

@Composable
fun ExactAlarmsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(text = stringResource(id = R.string.photo_widget_configure_interval_open_settings))
            }
        },
        modifier = modifier,
        title = {
            Text(text = stringResource(id = R.string.photo_widget_configure_interval_permission_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.photo_widget_configure_interval_permission_dialog_description))
        },
    )
}
