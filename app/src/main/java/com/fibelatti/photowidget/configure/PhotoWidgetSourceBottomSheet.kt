package com.fibelatti.photowidget.configure

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.RadioGroup
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetSourceBottomSheet(
    sheetState: AppSheetState,
    currentSource: PhotoWidgetSource,
    syncedDir: Set<Uri>,
    onDirRemoved: (Uri) -> Unit,
    onChangeSource: (PhotoWidgetSource) -> Unit,
) {
    AppBottomSheet(
        sheetState = sheetState,
    ) {
        SourcePickerContent(
            currentSource = currentSource,
            syncedDir = syncedDir,
            onDirRemoved = onDirRemoved,
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SourcePickerContent(
    currentSource: PhotoWidgetSource,
    syncedDir: Set<Uri>,
    onDirRemoved: (Uri) -> Unit,
    onConfirm: (PhotoWidgetSource) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.photo_widget_configure_menu_source),
        modifier = modifier,
    ) {
        var selection: PhotoWidgetSource by remember { mutableStateOf(currentSource) }
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
                }

                localResource.getString(stringRes)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        var dirList: List<Uri> by remember(syncedDir) { mutableStateOf(syncedDir.toList()) }
        if (currentSource == PhotoWidgetSource.DIRECTORY) {
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
                    .padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                dirList.forEachIndexed { index, dir ->
                    DirListItem(
                        dir = dir,
                        onRemoveClick = {
                            onDirRemoved(dir)
                            dirList = dirList - dir
                        },
                        backgroundShape = when (index) {
                            0 if dirList.size == 1 -> MaterialTheme.shapes.medium

                            0 if dirList.size > 1 -> MaterialTheme.shapes.medium.copy(
                                bottomStart = CornerSize(2.dp),
                                bottomEnd = CornerSize(2.dp),
                            )

                            dirList.lastIndex if dirList.size > 1 -> MaterialTheme.shapes.medium.copy(
                                topStart = CornerSize(2.dp),
                                topEnd = CornerSize(2.dp),
                            )

                            else -> RoundedCornerShape(2.dp)
                        },
                    )
                }
            }
        }

        WarningSign(
            text = stringResource(id = R.string.photo_widget_configure_source_warning),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

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
    dir: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    backgroundShape: Shape = MaterialTheme.shapes.medium,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onRemoveClick)
            .background(color = backgroundColor, shape = backgroundShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AutoSizeText(
            text = dir.lastPathSegment.orEmpty(),
            modifier = Modifier.weight(1f),
            color = contentColorFor(backgroundColor),
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge,
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_trash),
            contentDescription = null,
            tint = contentColorFor(backgroundColor),
        )
    }
}

// region Previews
@Composable
@AllPreviews
private fun SourcePickerContentPhotosPreview() {
    ExtendedTheme {
        SourcePickerContent(
            currentSource = PhotoWidgetSource.PHOTOS,
            syncedDir = emptySet(),
            onDirRemoved = {},
            onConfirm = {},
            onCancel = {},
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
            onConfirm = {},
            onCancel = {},
        )
    }
}
// endregion Previews
