package com.fibelatti.photowidget.home

import android.app.AlarmManager
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.BackgroundRestrictionBottomSheet
import com.fibelatti.photowidget.configure.ExactAlarmsDialog
import com.fibelatti.photowidget.platform.requestScheduleExactAlarmIntent
import com.fibelatti.photowidget.ui.icons.Alarm
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Appearance
import com.fibelatti.photowidget.ui.icons.Backup
import com.fibelatti.photowidget.ui.icons.Battery
import com.fibelatti.photowidget.ui.icons.Default
import com.fibelatti.photowidget.ui.icons.DynamicColor
import com.fibelatti.photowidget.ui.icons.HardDrive
import com.fibelatti.photowidget.ui.icons.KeepAlive
import com.fibelatti.photowidget.ui.icons.PrivacyPolicy
import com.fibelatti.photowidget.ui.icons.Question
import com.fibelatti.photowidget.ui.icons.Rate
import com.fibelatti.photowidget.ui.icons.Send
import com.fibelatti.photowidget.ui.icons.Settings
import com.fibelatti.photowidget.ui.icons.Translation
import com.fibelatti.photowidget.widget.PhotoWidgetRescheduleReceiver
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import com.google.android.material.color.DynamicColors

@Composable
fun SettingsScreen(
    onDefaultsClick: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    onKeepAliveClick: () -> Unit,
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
    val powerManager: PowerManager? = remember { localContext.getSystemService<PowerManager>() }

    var isBatteryUsageRestricted by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(localContext.packageName) != true)
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(AlarmManagerCompat.canScheduleExactAlarms(alarmManager))
    }

    var showExactAlarmsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    val checkExactAlarmBehaviorChange = {
        canScheduleExactAlarms = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
        if (canScheduleExactAlarms) {
            localContext.sendBroadcast(PhotoWidgetRescheduleReceiver.intent(localContext))
        }
    }
    val exactAlarmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        checkExactAlarmBehaviorChange()
    }

    val batteryOptimizationSheetState = rememberAppSheetState()

    SettingsScreen(
        onDefaultsClick = onDefaultsClick,
        onWidgetSettingsClick = onWidgetSettingsClick,
        onDataSaverClick = onDataSaverClick,
        canScheduleExactAlarms = canScheduleExactAlarms,
        onScheduleExactAlarmsClick = { showExactAlarmsDialog = true },
        isBatteryUsageRestricted = isBatteryUsageRestricted,
        onBatteryOptimizationClick = { batteryOptimizationSheetState.showBottomSheet() },
        onKeepAliveClick = onKeepAliveClick,
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
                exactAlarmsPermissionLauncher.launch(requestScheduleExactAlarmIntent(localContext))
                showExactAlarmsDialog = false
            },
        )
    }

    BackgroundRestrictionBottomSheet(
        sheetState = batteryOptimizationSheetState,
        onDismissRequest = {
            isBatteryUsageRestricted = powerManager?.isIgnoringBatteryOptimizations(localContext.packageName) != true
            checkExactAlarmBehaviorChange()
        },
    )
}

@Composable
private fun SettingsScreen(
    onDefaultsClick: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    canScheduleExactAlarms: Boolean,
    onScheduleExactAlarmsClick: () -> Unit,
    isBatteryUsageRestricted: Boolean,
    onBatteryOptimizationClick: () -> Unit,
    onKeepAliveClick: () -> Unit,
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
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = footerHeight + 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsSectionHeader(
                text = R.string.photo_widget_home_section_widgets,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Default),
                label = R.string.widget_defaults_title,
                onClick = onDefaultsClick,
                description = R.string.widget_defaults_description,
                shape = Shapes.TopShape,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Settings),
                label = R.string.widget_settings_title,
                onClick = onWidgetSettingsClick,
                description = R.string.widget_settings_description,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.HardDrive),
                label = R.string.photo_widget_home_data_saver,
                onClick = onDataSaverClick,
                description = R.string.photo_widget_home_data_saver_description,
                shape = Shapes.BottomShape,
            )

            SettingsSectionHeader(
                text = R.string.photo_widget_home_section_background_activity,
            )

            AnimatedVisibility(visible = !canScheduleExactAlarms) {
                SettingsListItem(
                    icon = rememberVectorPainter(AppIcons.Alarm),
                    label = R.string.photo_widget_configure_interval_grant_permission,
                    onClick = onScheduleExactAlarmsClick,
                    description = R.string.photo_widget_configure_interval_grant_permission_description,
                    shape = Shapes.TopShape,
                )
            }

            AnimatedVisibility(visible = isBatteryUsageRestricted) {
                SettingsListItem(
                    icon = rememberVectorPainter(AppIcons.Battery),
                    label = R.string.photo_widget_configure_battery_optimization,
                    onClick = onBatteryOptimizationClick,
                    description = R.string.photo_widget_configure_battery_optimization_description,
                    shape = if (!canScheduleExactAlarms) Shapes.MiddleShape else Shapes.TopShape,
                )
            }

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.KeepAlive),
                label = R.string.photo_widget_keep_alive_service_dialog_title,
                onClick = onKeepAliveClick,
                description = R.string.photo_widget_home_background_service_description,
                shape = if (!canScheduleExactAlarms || isBatteryUsageRestricted) {
                    Shapes.BottomShape
                } else {
                    Shapes.StandaloneShape
                },
            )

            SettingsSectionHeader(
                text = R.string.photo_widget_home_section_data,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Backup),
                label = R.string.photo_widget_home_backup,
                onClick = onBackupClick,
                description = R.string.photo_widget_home_backup_description,
                shape = Shapes.StandaloneShape,
            )

            SettingsSectionHeader(
                text = R.string.photo_widget_home_section_appearance,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Appearance),
                label = R.string.photo_widget_home_appearance,
                onClick = onAppearanceClick,
                shape = Shapes.TopShape,
            )

            if (DynamicColors.isDynamicColorAvailable() || LocalInspectionMode.current) {
                SettingsListItem(
                    icon = rememberVectorPainter(AppIcons.DynamicColor),
                    label = R.string.photo_widget_home_dynamic_colors,
                    onClick = onColorsClick,
                )
            }

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Translation),
                label = R.string.photo_widget_home_translations,
                onClick = onAppLanguageClick,
                shape = Shapes.BottomShape,
            )

            SettingsSectionHeader(
                text = R.string.photo_widget_home_section_about,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Question),
                label = R.string.photo_widget_home_help_settings,
                onClick = onSendFeedbackClick,
                shape = Shapes.TopShape,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Rate),
                label = R.string.photo_widget_home_rate,
                onClick = onRateClick,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.Send),
                label = R.string.photo_widget_home_share,
                onClick = onShareClick,
            )

            SettingsListItem(
                icon = rememberVectorPainter(AppIcons.PrivacyPolicy),
                label = R.string.photo_widget_home_privacy_policy,
                onClick = onPrivacyPolicyClick,
                shape = Shapes.BottomShape,
            )
        }

        val localDensity = LocalDensity.current
        SettingsFooter(
            onViewLicensesClick = onViewLicensesClick,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    footerHeight = with(localDensity) { coordinates.size.height.toDp() }
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
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {},
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
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onViewLicensesClick, role = Role.Button)
                .padding(all = 8.dp),
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
private fun SettingsSectionHeader(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = text),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(top = 26.dp, bottom = 2.dp),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SettingsListItem(
    icon: Painter,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(2.dp),
    @StringRes description: Int? = null,
) {
    ListItem(
        headlineText = stringResource(id = label),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        supportingText = description?.let { stringResource(id = description) },
        leadingContent = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        shape = shape,
    )
}

// region Previews
@Composable
@PreviewAll
private fun SettingsScreenPreview() {
    ExtendedTheme {
        SettingsScreen(
            onDefaultsClick = {},
            onWidgetSettingsClick = {},
            onDataSaverClick = {},
            canScheduleExactAlarms = false,
            onScheduleExactAlarmsClick = {},
            isBatteryUsageRestricted = true,
            onBatteryOptimizationClick = {},
            onKeepAliveClick = {},
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
