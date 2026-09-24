package com.fibelatti.photowidget.preferences

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.widget.PhotoWidgetSyncWorker
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.SliderItem
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun FolderSyncIntervalBottomSheet(
    sheetState: AppSheetState,
    currentValue: Int,
    onApplyClick: (newInterval: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        FolderSyncIntervalContent(
            currentValue = currentValue,
            onApplyClick = { newValue: Int ->
                onApplyClick(newValue)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
fun FolderSyncIntervalContent(
    currentValue: Int,
    onApplyClick: (newInterval: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(R.string.widget_settings_folder_sync_interval),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.widget_settings_folder_sync_interval_description),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )

        val sliderState: SliderState = rememberSliderState(
            value = currentValue.toFloat(),
            trackRange = PhotoWidgetSyncWorker.MIN_INTERVAL_HOURS.toFloat()
                .rangeTo(PhotoWidgetSyncWorker.MAX_INTERVAL_HOURS.toFloat()),
        )
        val resources: Resources = LocalResources.current

        SliderItem(
            state = sliderState,
            displayValueTransformation = { value ->
                folderSyncIntervalLabel(value = value.fastRoundToInt(), resources = resources)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Button(
            onClick = { onApplyClick(sliderState.value.fastRoundToInt()) },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}

fun folderSyncIntervalLabel(value: Int, resources: Resources): String {
    return resources.getQuantityString(
        R.plurals.photo_widget_configure_interval_current_hours,
        value,
        value,
    )
}

@PreviewAll
@Composable
private fun FolderSyncIntervalContentPreview() {
    ExtendedTheme {
        FolderSyncIntervalContent(
            currentValue = PhotoWidgetSyncWorker.DEFAULT_INTERVAL_HOURS,
            onApplyClick = {},
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
        )
    }
}
