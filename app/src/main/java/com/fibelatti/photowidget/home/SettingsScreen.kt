@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.home

import android.app.AlarmManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.ExactAlarmsDialog
import com.fibelatti.photowidget.platform.requestScheduleExactAlarmIntent
import com.fibelatti.photowidget.widget.PhotoWidgetRescheduleReceiver
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import com.google.android.material.color.DynamicColors

@Composable
fun SettingsScreen(
    onDefaultsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val alarmManager: AlarmManager = remember { requireNotNull(localContext.getSystemService()) }

    var canScheduleExactAlarms by remember {
        mutableStateOf(AlarmManagerCompat.canScheduleExactAlarms(alarmManager))
    }
    var showExactAlarmsDialog by remember {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        canScheduleExactAlarms = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
        if (canScheduleExactAlarms) {
            localContext.sendBroadcast(PhotoWidgetRescheduleReceiver.intent(localContext))
        }
    }

    SettingsScreen(
        onDefaultsClick = onDefaultsClick,
        onDataSaverClick = onDataSaverClick,
        canScheduleExactAlarms = canScheduleExactAlarms,
        onScheduleExactAlarmsClick = { showExactAlarmsDialog = true },
        onAppearanceClick = onAppearanceClick,
        onColorsClick = onColorsClick,
        onAppLanguageClick = onAppLanguageClick,
        onBackupClick = onBackupClick,
        onSendFeedbackClick = onSendFeedbackClick,
        onRateClick = onRateClick,
        onShareClick = onShareClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onViewLicensesClick = onViewLicensesClick,
        modifier = modifier,
    )

    if (showExactAlarmsDialog) {
        ExactAlarmsDialog(
            onDismiss = {
                showExactAlarmsDialog = false
            },
            onConfirm = {
                launcher.launch(requestScheduleExactAlarmIntent(localContext))
                showExactAlarmsDialog = false
            },
        )
    }
}

@Composable
private fun SettingsScreen(
    onDefaultsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    canScheduleExactAlarms: Boolean,
    onScheduleExactAlarmsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        var footerHeight by remember { mutableStateOf(64.dp) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 16.dp,
                    bottom = footerHeight + 16.dp,
                ),
        ) {
            SettingsAction(
                icon = R.drawable.ic_default,
                label = R.string.widget_defaults_title,
                onClick = onDefaultsClick,
            )

            SettingsAction(
                icon = R.drawable.ic_hard_drive,
                label = R.string.photo_widget_home_data_saver,
                onClick = onDataSaverClick,
            )

            AnimatedVisibility(visible = !canScheduleExactAlarms) {
                SettingsAction(
                    icon = R.drawable.ic_alarm,
                    label = R.string.photo_widget_configure_interval_grant_permission,
                    onClick = onScheduleExactAlarmsClick,
                )
            }

            SettingsAction(
                icon = R.drawable.ic_appearance,
                label = R.string.photo_widget_home_appearance,
                onClick = onAppearanceClick,
            )

            if (DynamicColors.isDynamicColorAvailable() || LocalInspectionMode.current) {
                SettingsAction(
                    icon = R.drawable.ic_dynamic_color,
                    label = R.string.photo_widget_home_dynamic_colors,
                    onClick = onColorsClick,
                )
            }

            SettingsAction(
                icon = R.drawable.ic_translation,
                label = R.string.photo_widget_home_translations,
                onClick = onAppLanguageClick,
            )

            SettingsAction(
                icon = R.drawable.ic_backup,
                label = R.string.photo_widget_home_backup,
                onClick = onBackupClick,
            )

            HorizontalDivider()

            SettingsAction(
                icon = R.drawable.ic_question,
                label = R.string.photo_widget_home_help,
                onClick = onSendFeedbackClick,
            )

            SettingsAction(
                icon = R.drawable.ic_rate,
                label = R.string.photo_widget_home_rate,
                onClick = onRateClick,
            )

            SettingsAction(
                icon = R.drawable.ic_share,
                label = R.string.photo_widget_home_share,
                onClick = onShareClick,
            )

            HorizontalDivider()

            SettingsAction(
                icon = R.drawable.ic_privacy_policy,
                label = R.string.photo_widget_home_privacy_policy,
                onClick = onPrivacyPolicyClick,
            )
        }

        val localDensity = LocalDensity.current
        SettingsFooter(
            onViewLicensesClick = onViewLicensesClick,
            modifier = Modifier
                .onLayoutRectChanged {
                    footerHeight = with(localDensity) { it.height.toDp() }
                }
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            0.4f to MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .padding(top = 30.dp, bottom = 16.dp)
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SettingsFooter(
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )

        Text(
            text = stringResource(id = R.string.photo_widget_home_developer),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge,
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(
                    onClick = onViewLicensesClick,
                    role = Role.Button,
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.photo_widget_home_version, BuildConfig.VERSION_NAME),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_view_licenses),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SettingsAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .minimumInteractiveComponentSize(),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(16.dp))

        AutoSizeText(
            text = stringResource(id = label),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            minTextSize = 8.sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

// region Previews
@Composable
@AllPreviews
private fun SettingsScreenPreview() {
    ExtendedTheme {
        SettingsScreen(
            onDefaultsClick = {},
            onDataSaverClick = {},
            canScheduleExactAlarms = false,
            onScheduleExactAlarmsClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onAppLanguageClick = {},
            onBackupClick = {},
            onSendFeedbackClick = {},
            onRateClick = {},
            onShareClick = {},
            onPrivacyPolicyClick = {},
            onViewLicensesClick = {},
        )
    }
}
// endregion Previews
