package com.fibelatti.photowidget.configure

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet
import com.fibelatti.ui.foundation.data

@Composable
fun RecentlyDeletedPhotoBottomSheet(
    sheetState: AppSheetState,
    onRestore: (localPhoto: LocalPhoto) -> Unit,
    onDelete: (localPhoto: LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localResources: Resources = LocalResources.current
    val photo: LocalPhoto = sheetState.data() ?: return

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = PhotoOptions.entries,
        optionName = { option -> localResources.getString(option.label) },
        onOptionSelected = { option ->
            when (option) {
                PhotoOptions.RESTORE -> onRestore(photo)
                PhotoOptions.DELETE -> onDelete(photo)
            }
        },
        modifier = modifier,
        header = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ShapedPhoto(
                    photo = photo,
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    shapeId = PhotoWidget.DEFAULT_SHAPE_ID,
                    cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                    modifier = Modifier.size(80.dp),
                )
            }
        },
    )
}

private enum class PhotoOptions(
    @StringRes val label: Int,
) {

    RESTORE(label = R.string.photo_widget_action_restore),

    DELETE(label = R.string.photo_widget_action_delete_permanently),
}
