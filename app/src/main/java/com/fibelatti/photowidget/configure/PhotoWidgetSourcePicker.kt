package com.fibelatti.photowidget.configure

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

object PhotoWidgetSourcePicker {

    fun show(
        context: Context,
        currentSource: PhotoWidgetSource,
        syncedDir: Set<Uri>,
        onDirRemoved: (Uri) -> Unit,
        onChangeSource: () -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            SourcePickerContent(
                currentSource = currentSource,
                syncedDir = syncedDir,
                onDirRemoved = onDirRemoved,
                onKeepSource = {
                    dismiss()
                },
                onChangeSource = {
                    onChangeSource()
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SourcePickerContent(
    currentSource: PhotoWidgetSource,
    syncedDir: Set<Uri>,
    onDirRemoved: (Uri) -> Unit,
    onKeepSource: () -> Unit,
    onChangeSource: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        var dirList by remember(syncedDir) { mutableStateOf(syncedDir.toList()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .nestedScroll(rememberNestedScrollInteropConnection()),
            contentPadding = PaddingValues(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            stickyHeader {
                Text(
                    text = stringResource(id = R.string.photo_widget_configure_menu_source),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.photo_widget_configure_source_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Text(
                        text = stringResource(
                            id = when (currentSource) {
                                PhotoWidgetSource.PHOTOS -> {
                                    R.string.photo_widget_configure_source_selection_photos
                                }

                                PhotoWidgetSource.DIRECTORY -> {
                                    if (dirList.isEmpty()) {
                                        R.string.photo_widget_configure_source_selection_directory_empty
                                    } else {
                                        R.string.photo_widget_configure_source_selection_directory_non_empty
                                    }
                                }
                            },
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (PhotoWidgetSource.DIRECTORY == currentSource) {
                items(dirList, key = { it }) { dir ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable {
                                onDirRemoved(dir)
                                dirList = dirList - dir
                            }
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(size = 8.dp),
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AutoSizeText(
                            text = dir.lastPathSegment.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.1f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
                            0.2f to MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(top = 30.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ButtonGroup(
                overflowIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }

                        OutlinedButton(
                            onClick = onKeepSource,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .animateWidth(interactionSource),
                            interactionSource = interactionSource,
                        ) {
                            AutoSizeText(
                                text = stringResource(id = R.string.photo_widget_configure_source_keep_current),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    },
                    menuContent = {},
                )

                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }

                        FilledTonalButton(
                            onClick = onChangeSource,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .animateWidth(interactionSource),
                            interactionSource = interactionSource,
                        ) {
                            AutoSizeText(
                                text = stringResource(
                                    id = when (currentSource) {
                                        PhotoWidgetSource.PHOTOS -> R.string.photo_widget_configure_source_set_directory
                                        PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_configure_source_set_photos
                                    },
                                ),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    },
                    menuContent = {},
                )
            }

            Text(
                text = stringResource(id = R.string.photo_widget_configure_source_warning),
                modifier = Modifier.padding(start = 40.dp, top = 8.dp, end = 40.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
@AllPreviews
private fun SourcePickerContentPhotosPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.PHOTOS,
            syncedDir = emptySet(),
            onDirRemoved = {},
            onKeepSource = {},
            onChangeSource = {},
        )
    }
}

@Composable
@AllPreviews
private fun SourcePickerContentDirectoryPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.DIRECTORY,
            syncedDir = List(10) { "https://test/$it".toUri() }.toSet(),
            onDirRemoved = {},
            onKeepSource = {},
            onChangeSource = {},
        )
    }
}
