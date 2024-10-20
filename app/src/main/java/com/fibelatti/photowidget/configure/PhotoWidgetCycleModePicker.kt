package com.fibelatti.photowidget.configure

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PhotoWidgetCycleModePicker {

    fun show(
        context: Context,
        cycleMode: PhotoWidgetCycleMode,
        onApplyClick: (newMode: PhotoWidgetCycleMode) -> Unit,
    ) {
        val alarmManager: AlarmManager = requireNotNull(context.getSystemService())

        ComposeBottomSheetDialog(context) {
            var canScheduleExactAlarms by remember {
                mutableStateOf(AlarmManagerCompat.canScheduleExactAlarms(alarmManager))
            }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                canScheduleExactAlarms = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
            }

            PhotoCycleModePickerContent(
                cycleMode = cycleMode,
                canScheduleExactAlarms = canScheduleExactAlarms,
                onOpenPermission = {
                    val intent = Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM").apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }

                    launcher.launch(intent)
                },
                onApplyClick = { newMode ->
                    onApplyClick(newMode)
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
private fun PhotoCycleModePickerContent(
    cycleMode: PhotoWidgetCycleMode,
    canScheduleExactAlarms: Boolean,
    onOpenPermission: () -> Unit,
    onApplyClick: (newMode: PhotoWidgetCycleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(cycleMode) }
    var showExplainerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_select_cycling_mode),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.size(8.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val borderColor = SegmentedButtonDefaults.borderStroke(SegmentedButtonDefaults.colors().activeBorderColor)

            SegmentedButton(
                selected = mode is PhotoWidgetCycleMode.Interval,
                onClick = {
                    mode = PhotoWidgetCycleMode.Interval(
                        loopingInterval = (cycleMode as? PhotoWidgetCycleMode.Interval)?.loopingInterval
                            ?: PhotoWidgetLoopingInterval.ONE_DAY,
                    )
                },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_cycling_mode_interval),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = mode is PhotoWidgetCycleMode.Schedule,
                onClick = {
                    mode = PhotoWidgetCycleMode.Schedule(
                        triggers = (cycleMode as? PhotoWidgetCycleMode.Schedule)?.triggers.orEmpty(),
                    )
                },
                shape = RectangleShape,
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_cycling_mode_schedule),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = mode is PhotoWidgetCycleMode.Disabled,
                onClick = { mode = PhotoWidgetCycleMode.Disabled },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                border = borderColor,
                label = {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_cycling_mode_disabled),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        AnimatedContent(
            targetState = mode,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.large,
                )
                .padding(all = 16.dp),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "SelectedCycleModeContent",
        ) { current ->
            when (current) {
                is PhotoWidgetCycleMode.Interval -> {
                    PhotoCycleModeIntervalContent(
                        photoWidgetCycleMode = current,
                        onApplyClick = onApplyClick,
                    )
                }

                is PhotoWidgetCycleMode.Schedule -> {
                    PhotoCycleModeScheduleContent(
                        photoWidgetCycleMode = current,
                        onApplyClick = onApplyClick,
                    )
                }

                is PhotoWidgetCycleMode.Disabled -> {
                    PhotoCycleModeDisabledContent(
                        onApplyClick = onApplyClick,
                    )
                }
            }
        }

        val showWarning = !canScheduleExactAlarms &&
            (mode is PhotoWidgetCycleMode.Schedule || mode is PhotoWidgetCycleMode.Interval)

        AnimatedVisibility(visible = showWarning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                )

                Text(
                    text = stringResource(id = R.string.photo_widget_configure_interval_warning),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )

                OutlinedButton(
                    onClick = { showExplainerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.photo_widget_configure_interval_grant_permission),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (showExplainerDialog) {
            AlertDialog(
                onDismissRequest = { showExplainerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onOpenPermission()
                            showExplainerDialog = false
                        },
                    ) {
                        Text(text = stringResource(id = R.string.photo_widget_configure_interval_open_settings))
                    }
                },
                title = {
                    Text(text = stringResource(id = R.string.photo_widget_configure_interval_permission_dialog_title))
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.photo_widget_configure_interval_permission_dialog_description,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PhotoCycleModeIntervalContent(
    photoWidgetCycleMode: PhotoWidgetCycleMode.Interval,
    onApplyClick: (newMode: PhotoWidgetCycleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var interval by remember { mutableStateOf(photoWidgetCycleMode.loopingInterval) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_configure_cycling_mode_interval_description),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Slider(
                value = interval.repeatInterval.toFloat(),
                onValueChange = { newValue -> interval = interval.copy(repeatInterval = newValue.toLong()) },
                modifier = Modifier.weight(1f),
                valueRange = interval.range(),
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = "${interval.repeatInterval}",
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val borderColor = SegmentedButtonDefaults.borderStroke(SegmentedButtonDefaults.colors().activeBorderColor)

            SegmentedButton(
                selected = TimeUnit.SECONDS == interval.timeUnit,
                onClick = {
                    interval = interval.copy(
                        repeatInterval = interval.repeatInterval.coerceAtLeast(
                            minimumValue = PhotoWidgetLoopingInterval.MIN_SECONDS,
                        ),
                        timeUnit = TimeUnit.SECONDS,
                    )
                },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                border = borderColor,
                label = {
                    AutoSizeText(
                        text = stringResource(id = R.string.photo_widget_configure_interval_seconds_label),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = TimeUnit.MINUTES == interval.timeUnit,
                onClick = { interval = interval.copy(timeUnit = TimeUnit.MINUTES) },
                shape = RectangleShape,
                border = borderColor,
                label = {
                    AutoSizeText(
                        text = stringResource(id = R.string.photo_widget_configure_interval_minutes_label),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = TimeUnit.HOURS == interval.timeUnit,
                onClick = {
                    interval = interval.copy(
                        repeatInterval = interval.repeatInterval.coerceAtMost(
                            maximumValue = PhotoWidgetLoopingInterval.MAX_HOURS,
                        ),
                        timeUnit = TimeUnit.HOURS,
                    )
                },
                shape = RectangleShape,
                border = borderColor,
                label = {
                    AutoSizeText(
                        text = stringResource(id = R.string.photo_widget_configure_interval_hours_label),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )

            SegmentedButton(
                selected = TimeUnit.DAYS == interval.timeUnit,
                onClick = { interval = interval.copy(timeUnit = TimeUnit.DAYS) },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                border = borderColor,
                label = {
                    AutoSizeText(
                        text = stringResource(R.string.photo_widget_configure_interval_days_label),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
        }

        FilledTonalButton(
            onClick = {
                onApplyClick(PhotoWidgetCycleMode.Interval(loopingInterval = interval))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.photo_widget_action_apply),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PhotoCycleModeScheduleContent(
    photoWidgetCycleMode: PhotoWidgetCycleMode.Schedule,
    onApplyClick: (newMode: PhotoWidgetCycleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val triggers = remember { photoWidgetCycleMode.triggers.toMutableStateList() }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (triggers.isNotEmpty()) {
            Text(
                text = stringResource(R.string.photo_widget_configure_cycling_mode_schedule_description),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )

            for (item in triggers) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { triggers.remove(item) }
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.asString(),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        modifier = Modifier.size(12.dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.photo_widget_configure_schedule_placeholder),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (triggers.size < 4) {
            TextButton(
                onClick = { showTimePickerDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.photo_widget_configure_schedule_add_new))
            }
        } else {
            Text(
                text = stringResource(R.string.photo_widget_configure_schedule_limit_reached),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        FilledTonalButton(
            onClick = { onApplyClick(PhotoWidgetCycleMode.Schedule(triggers = triggers.toSet())) },
            modifier = Modifier.fillMaxWidth(),
            enabled = triggers.isNotEmpty(),
        ) {
            Text(
                text = stringResource(id = R.string.photo_widget_action_apply),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showTimePickerDialog) {
        TimePickerDialog(
            onConfirm = { timePickerState ->
                val newEntry = Time(timePickerState.hour, timePickerState.minute)
                if (!triggers.contains(newEntry)) triggers.add(newEntry)

                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false },
        )
    }
}

@Composable
private fun PhotoCycleModeDisabledContent(
    onApplyClick: (newMode: PhotoWidgetCycleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_configure_cycling_mode_disabled_description),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
        )

        FilledTonalButton(
            onClick = { onApplyClick(PhotoWidgetCycleMode.Disabled) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.photo_widget_action_apply),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TimePickerDialog(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.photo_widget_time_picker_title),
                    style = MaterialTheme.typography.labelMedium,
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text(text = stringResource(R.string.photo_widget_action_cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(timePickerState) },
                    ) {
                        Text(text = stringResource(R.string.photo_widget_action_confirm))
                    }
                }
            }
        }
    }
}

// region Previews
@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun IntervalPreview() {
    ExtendedTheme {
        PhotoCycleModePickerContent(
            cycleMode = PhotoWidgetCycleMode.Interval(
                loopingInterval = PhotoWidgetLoopingInterval(
                    repeatInterval = 1,
                    timeUnit = TimeUnit.SECONDS,
                ),
            ),
            canScheduleExactAlarms = false,
            onOpenPermission = {},
            onApplyClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun ScheduleEmptyPreview() {
    ExtendedTheme {
        PhotoCycleModePickerContent(
            cycleMode = PhotoWidgetCycleMode.Schedule(
                triggers = emptySet(),
            ),
            canScheduleExactAlarms = false,
            onOpenPermission = {},
            onApplyClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun SchedulePreview() {
    ExtendedTheme {
        PhotoCycleModePickerContent(
            cycleMode = PhotoWidgetCycleMode.Schedule(
                triggers = setOf(
                    Time(hour = 8, minute = 0),
                    Time(hour = 12, minute = 0),
                    Time(hour = 16, minute = 0),
                    Time(hour = 20, minute = 0),
                ),
            ),
            canScheduleExactAlarms = false,
            onOpenPermission = {},
            onApplyClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun DisabledPreview() {
    ExtendedTheme {
        PhotoCycleModePickerContent(
            cycleMode = PhotoWidgetCycleMode.Disabled,
            canScheduleExactAlarms = false,
            onOpenPermission = {},
            onApplyClick = {},
        )
    }
}
// endregion Previews
