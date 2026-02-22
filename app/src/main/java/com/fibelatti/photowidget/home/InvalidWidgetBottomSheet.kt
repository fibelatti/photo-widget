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
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.data

@Composable
fun InvalidWidgetBottomSheet(
    sheetState: AppSheetState,
    onDelete: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val localResources: Resources = LocalResources.current
    val appWidgetId: Int = sheetState.data() ?: return

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = InvalidWidgetOptions.entries,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelected = { option ->
            when (option) {
                InvalidWidgetOptions.EDIT -> {
                    val intent = PhotoWidgetConfigureActivity.editWidgetIntent(
                        context = localContext,
                        appWidgetId = appWidgetId,
                    )

                    localContext.startActivity(intent)
                }

                InvalidWidgetOptions.DELETE -> {
                    onDelete(appWidgetId)
                }
            }
        },
        modifier = modifier,
        footer = {
            WarningSign(
                text = stringResource(R.string.photo_widget_home_invalid_widgets_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            )
        },
    )
}

private enum class InvalidWidgetOptions(
    @StringRes val label: Int,
) {

    EDIT(label = R.string.photo_widget_home_my_widget_action_edit),
    DELETE(label = R.string.photo_widget_home_removed_widget_action_delete),
}
