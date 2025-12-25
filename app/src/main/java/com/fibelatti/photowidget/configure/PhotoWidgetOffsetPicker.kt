@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.preferences.DefaultPicker
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
    DefaultPicker(
        title = stringResource(id = R.string.photo_widget_configure_offset),
        modifier = modifier,
    ) {
        var horizontalValue by remember(horizontalOffset) { mutableIntStateOf(horizontalOffset) }
        var verticalValue by remember(verticalOffset) { mutableIntStateOf(verticalOffset) }

        BoxWithConstraints {
            if (maxWidth < 600.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PhotoWidgetOffsetViewer(
                        horizontalValue = horizontalValue,
                        verticalValue = verticalValue,
                    )

                    PhotoWidgetOffsetControls(
                        horizontalValue = horizontalValue,
                        verticalValue = verticalValue,
                        onHorizontalValueChange = { horizontalValue = it },
                        onVerticalValueChange = { verticalValue = it },
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhotoWidgetOffsetViewer(
                        horizontalValue = horizontalValue,
                        verticalValue = verticalValue,
                        modifier = Modifier.weight(0.4f),
                    )

                    PhotoWidgetOffsetControls(
                        horizontalValue = horizontalValue,
                        verticalValue = verticalValue,
                        onHorizontalValueChange = { horizontalValue = it },
                        onVerticalValueChange = { verticalValue = it },
                        modifier = Modifier.weight(0.6f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    horizontalValue = 0
                    verticalValue = 0
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_action_reset))
            }

            Button(
                onClick = { onApplyClick(horizontalValue, verticalValue) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_action_apply))
            }
        }
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
private fun PhotoWidgetOffsetControls(
    horizontalValue: Int,
    verticalValue: Int,
    onHorizontalValueChange: (Int) -> Unit,
    onVerticalValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localHaptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .height(144.dp)
                    .width(48.dp),
            ) {
                FilledTonalIconButton(
                    onClick = {
                        onVerticalValueChange(verticalValue - 1)
                        localHaptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.rotate(180f),
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))

                FilledTonalIconButton(
                    onClick = {
                        onVerticalValueChange(verticalValue + 1)
                        localHaptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_down),
                        contentDescription = null,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .width(144.dp)
                    .height(48.dp),
            ) {
                FilledTonalIconButton(
                    onClick = {
                        onHorizontalValueChange(horizontalValue - 1)
                        localHaptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))

                FilledTonalIconButton(
                    onClick = {
                        onHorizontalValueChange(horizontalValue + 1)
                        localHaptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(
                    id = R.string.photo_widget_configure_offset_current_horizontal,
                    horizontalValue,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(
                    id = R.string.photo_widget_configure_offset_current_vertical,
                    verticalValue,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
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
