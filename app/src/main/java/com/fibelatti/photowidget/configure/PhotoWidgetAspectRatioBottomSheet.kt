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
    onAspectRatioSelect: (PhotoWidgetAspectRatio) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        AspectRatioPickerContent(
            onAspectRatioSelect = { newAspectRatio ->
                onAspectRatioSelect(newAspectRatio)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
private fun AspectRatioPickerContent(
    onAspectRatioSelect: (PhotoWidgetAspectRatio) -> Unit,
) {
    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_aspect_ratio_title),
    ) {
        AspectRatioPicker(
            onAspectRatioSelect = onAspectRatioSelect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
