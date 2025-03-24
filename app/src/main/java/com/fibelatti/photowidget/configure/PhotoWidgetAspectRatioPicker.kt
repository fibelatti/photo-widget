package com.fibelatti.photowidget.configure

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.home.AspectRatioPicker
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog

object PhotoWidgetAspectRatioPicker {

    fun show(
        context: Context,
        onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            AspectRatioPickerContent(
                onAspectRatioSelected = { newAspectRatio ->
                    onAspectRatioSelected(newAspectRatio)
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
private fun AspectRatioPickerContent(
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_aspect_ratio_title),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        AspectRatioPicker(
            onAspectRatioSelected = onAspectRatioSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
