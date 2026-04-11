package com.fibelatti.photowidget.home

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.data

@Composable
fun RemovedWidgetBottomSheet(
    sheetState: AppSheetState,
    onKeep: (appWidgetId: Int) -> Unit,
    onDelete: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val localResources: Resources = LocalResources.current
    val data: RemovedWidgetBottomSheetData = sheetState.data() ?: return

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = buildList {
            add(RemovedWidgetOptions.RESTORE)
            if (PhotoWidgetStatus.KEPT != data.status) {
                add(RemovedWidgetOptions.KEEP)
            }
            add(RemovedWidgetOptions.DELETE)
        },
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelect = { option ->
            when (option) {
                RemovedWidgetOptions.RESTORE -> {
                    val intent = PhotoWidgetConfigureActivity.restoreWidgetIntent(
                        context = localContext,
                        appWidgetId = data.appWidgetId,
                    )

                    localContext.startActivity(intent)
                }

                RemovedWidgetOptions.KEEP -> {
                    onKeep(data.appWidgetId)
                }

                RemovedWidgetOptions.DELETE -> {
                    onDelete(data.appWidgetId)
                }
            }
        },
        modifier = modifier,
        footer = {
            WarningSign(
                text = stringResource(id = R.string.photo_widget_home_removed_widgets_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            )
        },
    )
}

class RemovedWidgetBottomSheetData(
    val appWidgetId: Int,
    val status: PhotoWidgetStatus,
)

private enum class RemovedWidgetOptions(
    @StringRes val label: Int,
) {

    RESTORE(label = R.string.photo_widget_home_removed_widget_action_restore),
    KEEP(label = R.string.photo_widget_home_removed_widget_action_keep),
    DELETE(label = R.string.photo_widget_home_removed_widget_action_delete),
}
