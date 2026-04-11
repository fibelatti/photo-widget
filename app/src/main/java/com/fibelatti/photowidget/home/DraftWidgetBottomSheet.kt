package com.fibelatti.photowidget.home

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.data

@Composable
fun DraftWidgetBottomSheet(
    sheetState: AppSheetState,
    onDelete: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val localResources: Resources = LocalResources.current
    val appWidgetId: Int = sheetState.data() ?: return

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = DraftWidgetOptions.entries,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelect = { option ->
            when (option) {
                DraftWidgetOptions.EDIT -> {
                    val intent = PhotoWidgetConfigureActivity.editWidgetIntent(
                        context = localContext,
                        appWidgetId = appWidgetId,
                    )

                    localContext.startActivity(intent)
                }

                DraftWidgetOptions.DISCARD -> {
                    onDelete(appWidgetId)
                }
            }
        },
        modifier = modifier,
    )
}

private enum class DraftWidgetOptions(
    @StringRes val label: Int,
) {

    EDIT(label = R.string.photo_widget_home_my_widget_action_edit),
    DISCARD(label = R.string.photo_widget_home_draft_action_discard),
}
