package com.fibelatti.photowidget.configure

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.SyncDir
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Check
import com.fibelatti.photowidget.ui.icons.ChevronDown
import com.fibelatti.photowidget.ui.icons.Trash
import com.fibelatti.ui.component.AppBottomSheet
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.component.RadioGroup
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetSourceBottomSheet(
    sheetState: AppSheetState,
    currentSource: PhotoWidgetSource,
    syncedDir: Set<SyncDir>,
    onDirRemove: (Uri) -> Unit,
    onDirSubdirectoriesChange: (Uri, Boolean) -> Unit,
    onChangeSource: (PhotoWidgetSource) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        SourcePickerContent(
            currentSource = currentSource,
            syncedDir = syncedDir,
            onDirRemove = onDirRemove,
            onDirSubdirectoriesChange = onDirSubdirectoriesChange,
            onConfirm = { newSource ->
                if (newSource != currentSource) {
                    onChangeSource(newSource)
                }
                sheetState.hideBottomSheet()
            },
            onCancel = sheetState::hideBottomSheet,
        )
    }
}

@Composable
private fun SourcePickerContent(
    currentSource: PhotoWidgetSource,
    syncedDir: Set<SyncDir>,
    onDirRemove: (Uri) -> Unit,
    onDirSubdirectoriesChange: (Uri, Boolean) -> Unit,
    onConfirm: (PhotoWidgetSource) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.photo_widget_configure_menu_source),
        modifier = modifier,
    ) {
        var selection: PhotoWidgetSource by rememberSaveable { mutableStateOf(currentSource) }
        val localResource = LocalResources.current

        RadioGroup(
            items = PhotoWidgetSource.entries,
            itemSelected = { source -> source == selection },
            onItemClick = { source -> selection = source },
            itemTitle = { source -> localResource.getString(source.label) },
            itemDescription = { source ->
                val stringRes = when (source) {
                    PhotoWidgetSource.PHOTOS -> R.string.photo_widget_source_photos_description
                    PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_source_directory_description
                    PhotoWidgetSource.GIF -> R.string.photo_widget_source_gif_description
                }

                localResource.getString(stringRes)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            itemFlag = { source ->
                if (source == PhotoWidgetSource.GIF) {
                    Text(
                        text = stringResource(R.string.warning_experimental),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
        )

        InformationalPanel(
            text = stringResource(id = R.string.photo_widget_configure_source_warning),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        var dirList: List<SyncDir> by remember(syncedDir) { mutableStateOf(syncedDir.toList()) }
        if (currentSource == PhotoWidgetSource.DIRECTORY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        id = if (dirList.isEmpty()) {
                            R.string.photo_widget_configure_source_selection_directory_empty
                        } else {
                            R.string.photo_widget_configure_source_selection_directory_non_empty
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall,
                )

                dirList.forEachIndexed { index, syncDir ->
                    DirListItem(
                        syncDir = syncDir,
                        onSubdirectoriesChange = { value ->
                            onDirSubdirectoriesChange(syncDir.dir, value)
                        },
                        onRemoveClick = {
                            onDirRemove(syncDir.dir)
                            dirList = dirList - syncDir
                        },
                        backgroundShape = when (index) {
                            0 if dirList.size == 1 -> Shapes.StandaloneShape
                            0 if dirList.size > 1 -> Shapes.TopShape
                            dirList.lastIndex if dirList.size > 1 -> Shapes.BottomShape
                            else -> Shapes.MiddleShape
                        },
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
            val interactionSources: Array<MutableInteractionSource> = remember {
                Array(size = 2) { MutableInteractionSource() }
            }

            OutlinedButton(
                onClick = onCancel,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                interactionSource = interactionSources[0],
            ) {
                Text(text = stringResource(R.string.photo_widget_action_cancel))
            }

            Button(
                onClick = { onConfirm(selection) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                interactionSource = interactionSources[1],
            ) {
                Text(text = stringResource(R.string.photo_widget_action_confirm))
            }
        }
    }
}

@Composable
private fun DirListItem(
    syncDir: SyncDir,
    onSubdirectoriesChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    backgroundShape: Shape = Shapes.StandaloneShape,
) {
    val contentColor: Color = contentColorFor(backgroundColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = backgroundShape)
            .padding(all = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AutoSizeText(
                text = syncDir.dir.lastPathSegment.orEmpty(),
                color = contentColor,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
            )

            SyncScopeChip(
                subdirectories = syncDir.subdirectories,
                onSubdirectoriesChange = onSubdirectoriesChange,
            )
        }

        OutlinedIconButton(
            onClick = onRemoveClick,
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                imageVector = AppIcons.Trash,
                contentDescription = stringResource(R.string.photo_widget_configure_menu_remove),
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun SyncScopeChip(
    subdirectories: Boolean,
    onSubdirectoriesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu: Boolean by rememberSaveable { mutableStateOf(false) }
    val backgroundColor: Color = MaterialTheme.colorScheme.surface
    val contentColor: Color = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(color = backgroundColor)
                .heightIn(min = 40.dp)
                .clickable { showMenu = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(syncScopeLabel(subdirectories = subdirectories)),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
            )

            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.photo_widget_configure_source_sync_scope),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )

            for (option in listOf(true, false)) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(syncScopeLabel(subdirectories = option))) },
                    onClick = {
                        showMenu = false
                        if (option != subdirectories) {
                            onSubdirectoriesChange(option)
                        }
                    },
                    trailingIcon = {
                        if (option == subdirectories) {
                            Icon(imageVector = AppIcons.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

@StringRes
private fun syncScopeLabel(subdirectories: Boolean): Int {
    return if (subdirectories) {
        R.string.photo_widget_configure_source_sync_scope_subfolders
    } else {
        R.string.photo_widget_configure_source_sync_scope_folder_only
    }
}

// region Previews
@Composable
@PreviewAll
private fun SourcePickerContentPhotosPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.PHOTOS,
            syncedDir = emptySet(),
            onDirRemove = {},
            onDirSubdirectoriesChange = { _, _ -> },
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Composable
@PreviewAll
private fun SourcePickerContentDirectoryPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.DIRECTORY,
            syncedDir = List(10) { index ->
                SyncDir(dir = "https://test/$index".toUri(), subdirectories = index % 2 == 0)
            }.toSet(),
            onDirRemove = {},
            onDirSubdirectoriesChange = { _, _ -> },
            onConfirm = {},
            onCancel = {},
        )
    }
}
// endregion Previews
