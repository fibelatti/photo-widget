package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.home.AspectRatioPicker
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet

@Composable
fun PhotoWidgetAspectRatioBottomSheet(
    sheetState: AppSheetState,
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        AspectRatioPickerContent(
            onAspectRatioSelected = { newAspectRatio ->
                onAspectRatioSelected(newAspectRatio)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
private fun AspectRatioPickerContent(
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
) {
    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_aspect_ratio_title),
    ) {
        AspectRatioPicker(
            onAspectRatioSelected = onAspectRatioSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
