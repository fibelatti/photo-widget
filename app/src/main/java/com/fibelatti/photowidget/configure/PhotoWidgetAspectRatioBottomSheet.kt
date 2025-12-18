package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.home.AspectRatioPicker
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.text.AutoSizeText

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AutoSizeText(
                text = stringResource(R.string.hint_scroll_to_view_more),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
