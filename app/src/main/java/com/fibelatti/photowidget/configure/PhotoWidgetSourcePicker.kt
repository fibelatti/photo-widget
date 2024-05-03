package com.fibelatti.photowidget.configure

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

object PhotoWidgetSourcePicker {

    fun show(
        context: Context,
        currentSource: PhotoWidgetSource,
        syncedDir: Set<Uri>,
        onDirRemoved: (Uri) -> Unit,
        onApplyClick: () -> Unit,
    ) {
        ComposeBottomSheetDialog(context) {
            SourcePickerContent(
                currentSource = currentSource,
                syncedDir = syncedDir,
                onDirRemoved = onDirRemoved,
                onApplyClick = {
                    onApplyClick()
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SourcePickerContent(
    currentSource: PhotoWidgetSource,
    syncedDir: Set<Uri>,
    onDirRemoved: (Uri) -> Unit,
    onApplyClick: () -> Unit,
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
            contentPadding = PaddingValues(bottom = 120.dp),
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
                        textAlign = TextAlign.Justify,
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
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (PhotoWidgetSource.DIRECTORY == currentSource) {
                items(dirList) { dir ->
                    Row(
                        modifier = Modifier
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
                        Text(
                            text = dir.lastPathSegment.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ),
                    ),
            )

            FilledTonalButton(
                onClick = onApplyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(
                        id = when (currentSource) {
                            PhotoWidgetSource.PHOTOS -> R.string.photo_widget_configure_source_set_directory
                            PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_configure_source_set_photos
                        },
                    ),
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
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun SourcePickerContentPhotosPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.PHOTOS,
            syncedDir = emptySet(),
            onDirRemoved = {},
            onApplyClick = {},
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun SourcePickerContentDirectoryPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.DIRECTORY,
            syncedDir = List(10) { Uri.parse("https://test/$it") }.toSet(),
            onDirRemoved = {},
            onApplyClick = {},
        )
    }
}
