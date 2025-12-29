@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.DefaultSheetFooterButtons
import com.fibelatti.photowidget.ui.LocalSamplePhoto
import com.fibelatti.photowidget.ui.NumberSpinner
import com.fibelatti.photowidget.ui.WidgetPositionViewer
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetOffsetPicker(
    horizontalOffset: Int,
    verticalOffset: Int,
    onApplyClick: (newHorizontalOffset: Int, newVerticalOffset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.photo_widget_configure_offset),
        modifier = modifier,
    ) {
        var horizontalValue by remember(horizontalOffset) { mutableIntStateOf(horizontalOffset) }
        var verticalValue by remember(verticalOffset) { mutableIntStateOf(verticalOffset) }

        PhotoWidgetOffsetViewer(
            horizontalValue = horizontalValue,
            verticalValue = verticalValue,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.photo_widget_configure_offset_current_horizontal),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )

                NumberSpinner(
                    value = horizontalValue,
                    onIncreaseClick = { horizontalValue++ },
                    onDecreaseClick = { horizontalValue-- },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = R.string.photo_widget_configure_offset_current_vertical),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )

                NumberSpinner(
                    value = verticalValue,
                    onIncreaseClick = { verticalValue++ },
                    onDecreaseClick = { verticalValue-- },
                )
            }
        }

        DefaultSheetFooterButtons(
            onApplyClick = { onApplyClick(horizontalValue, verticalValue) },
            onResetClick = {
                horizontalValue = 0
                verticalValue = 0
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun PhotoWidgetOffsetViewer(
    horizontalValue: Int,
    verticalValue: Int,
    modifier: Modifier = Modifier,
) {
    WidgetPositionViewer(
        photoWidget = PhotoWidget(
            currentPhoto = LocalSamplePhoto.current,
            verticalOffset = verticalValue,
            horizontalOffset = horizontalValue,
        ),
        modifier = modifier
            .padding(16.dp)
            .width(200.dp)
            .aspectRatio(.75f),
    )
}

@Composable
@AllPreviews
private fun OffsetPickerPreview() {
    ExtendedTheme {
        PhotoWidgetOffsetPicker(
            horizontalOffset = 0,
            verticalOffset = 0,
            onApplyClick = { _, _ -> },
        )
    }
}
