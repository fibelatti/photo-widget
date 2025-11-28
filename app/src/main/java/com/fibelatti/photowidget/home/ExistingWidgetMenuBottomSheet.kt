package com.fibelatti.photowidget.home

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.data

@Composable
fun ExistingWidgetMenuBottomSheet(
    sheetState: AppSheetState,
    onSync: (appWidgetId: Int) -> Unit,
    onLock: (appWidgetId: Int) -> Unit,
    onUnlock: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext: Context = LocalContext.current
    val localResources = LocalResources.current
    val data: ExistingWidgetMenuBottomSheetData = sheetState.data() ?: return

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = MyWidgetOptions.options(canSync = data.canSync, canLock = data.canLock, isLocked = data.isLocked),
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelected = { option ->
            when (option) {
                MyWidgetOptions.SYNC_PHOTOS -> {
                    onSync(data.appWidgetId)

                    Toast.makeText(
                        localContext,
                        R.string.photo_widget_home_my_widget_syncing_feedback,
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                MyWidgetOptions.EDIT -> {
                    val intent = PhotoWidgetConfigureActivity.editWidgetIntent(
                        context = localContext,
                        appWidgetId = data.appWidgetId,
                    )

                    localContext.startActivity(intent)
                }

                MyWidgetOptions.DUPLICATE -> {
                    val intent = PhotoWidgetConfigureActivity.duplicateWidgetIntent(
                        context = localContext,
                        appWidgetId = data.appWidgetId,
                    )

                    localContext.startActivity(intent)
                }

                MyWidgetOptions.LOCK -> {
                    onLock(data.appWidgetId)
                }

                MyWidgetOptions.UNLOCK -> {
                    onUnlock(data.appWidgetId)
                }
            }
        },
        modifier = modifier,
        footer = {
            if (data.canLock) {
                Text(
                    text = stringResource(R.string.photo_widget_home_my_widget_lock_explainer),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

class ExistingWidgetMenuBottomSheetData(
    val appWidgetId: Int,
    val canSync: Boolean,
    val canLock: Boolean,
    val isLocked: Boolean,
)

private enum class MyWidgetOptions(
    @StringRes val label: Int,
) {

    SYNC_PHOTOS(label = R.string.photo_widget_home_my_widget_action_sync),
    EDIT(label = R.string.photo_widget_home_my_widget_action_edit),
    DUPLICATE(label = R.string.photo_widget_home_my_widget_action_duplicate),
    LOCK(label = R.string.photo_widget_home_my_widget_action_lock),
    UNLOCK(label = R.string.photo_widget_home_my_widget_action_unlock),
    ;

    companion object {

        fun options(canSync: Boolean, canLock: Boolean, isLocked: Boolean): List<MyWidgetOptions> = buildList {
            if (canSync) {
                add(SYNC_PHOTOS)
            }

            add(EDIT)
            add(DUPLICATE)

            when {
                canLock && isLocked -> add(UNLOCK)
                canLock -> add(LOCK)
            }
        }
    }
}
