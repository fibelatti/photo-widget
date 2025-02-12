package com.fibelatti.photowidget.platform

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.ui.foundation.TextWithLinks
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

object BackgroundRestrictedSheetDialog {

    fun show(context: Context) {
        ComposeBottomSheetDialog(context) {
            BackgroundPickerContent(
                onOpenAppSettingsClick = {
                    context.startActivity(appSettingsIntent(context = context))
                },
                onOpenPowerOptimizationSettings = {
                    context.startActivity(batteryUsageSettingsIntent())
                },
            )
        }.show()
    }
}

@Composable
private fun BackgroundPickerContent(
    onOpenAppSettingsClick: () -> Unit,
    onOpenPowerOptimizationSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.restriction_warning_dialog_title),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.restriction_warning_dialog_body_1),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(R.string.restriction_warning_dialog_body_2),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )

        TextWithLinks(
            text = stringResource(R.string.restriction_warning_dialog_body_3),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            linkColor = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.size(8.dp))

        FilledTonalButton(
            onClick = onOpenAppSettingsClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.restriction_warning_open_app_settings))
        }

        FilledTonalButton(
            onClick = onOpenPowerOptimizationSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.restriction_warning_open_power_settings))
        }
    }
}

@Composable
@AllPreviews
private fun BackgroundPickerContentPreviews() {
    ExtendedTheme {
        BackgroundPickerContent(
            onOpenAppSettingsClick = {},
            onOpenPowerOptimizationSettings = {},
        )
    }
}
