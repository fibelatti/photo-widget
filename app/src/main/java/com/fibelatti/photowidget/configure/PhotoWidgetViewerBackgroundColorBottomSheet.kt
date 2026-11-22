package com.fibelatti.photowidget.configure

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.rememberSampleBitmap
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

private const val FALLBACK_COLOR_HEX = "000000"

@Composable
fun PhotoWidgetViewerBackgroundColorBottomSheet(
    sheetState: AppSheetState,
    currentColorHex: String?,
    onApplyClick: (String) -> Unit,
    onResetClick: () -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        ViewerBackgroundColorContent(
            currentColorHex = currentColorHex,
            onApplyClick = { hex ->
                onApplyClick(hex)
                sheetState.hideBottomSheet()
            },
            onResetClick = {
                onResetClick()
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
private fun ViewerBackgroundColorContent(
    currentColorHex: String?,
    onApplyClick: (String) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var colorHex: String by rememberSaveable { mutableStateOf(currentColorHex ?: FALLBACK_COLOR_HEX) }
    val sampleBitmap: Bitmap = rememberSampleBitmap()

    DefaultSheetContent(
        title = stringResource(R.string.photo_widget_configure_tap_action_viewer_background_color),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorPickerContent(
                currentColorHex = colorHex,
                onColorChange = { newValue: String -> colorHex = newValue },
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 200.dp)
                        .aspectRatio(1f)
                        .background(
                            color = Color("#$colorHex".toColorInt()),
                            shape = MaterialTheme.shapes.small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val cornerRadius: Float = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx()

                    Image(
                        bitmap = remember(sampleBitmap, cornerRadius) {
                            sampleBitmap.withRoundedCorners(radius = cornerRadius).asImageBitmap()
                        },
                        contentDescription = null,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onResetClick,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    AutoSizeText(
                        text = stringResource(
                            R.string.photo_widget_configure_tap_action_viewer_background_color_default,
                        ),
                        maxLines = 1,
                    )
                }

                Button(
                    onClick = { onApplyClick(colorHex) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.photo_widget_action_apply))
                }
            }
        }
    }
}

@PreviewAll
@Composable
private fun ViewerBackgroundColorContentPreview() {
    ExtendedTheme {
        ViewerBackgroundColorContent(
            currentColorHex = null,
            onApplyClick = {},
            onResetClick = {},
        )
    }
}
