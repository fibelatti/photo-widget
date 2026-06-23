package com.fibelatti.photowidget.configure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.model.canSort
import com.fibelatti.photowidget.model.orderedPhotosForDisplay
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.component.rememberAppSheetState
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun PhotoWidgetConfigureContentTab(
    viewModel: PhotoWidgetConfigureViewModel,
    onPickFromSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    val sourceSheetState: AppSheetState = rememberAppSheetState()
    val importFromWidgetSheetState: AppSheetState = rememberAppSheetState()
    val recentlyDeletedPhotoSheetState: AppSheetState = rememberAppSheetState()

    var showGifReplaceDialog: Boolean by rememberSaveable { mutableStateOf(false) }

    PhotoWidgetConfigureContentTab(
        photoWidget = state.photoWidget,
        onChangeSourceClick = sourceSheetState::showBottomSheet,
        isImportAvailable = state.isImportAvailable,
        onImportClick = importFromWidgetSheetState::showBottomSheet,
        onPickFromSourceClick = {
            if (state.photoWidget.source == PhotoWidgetSource.GIF && state.photoWidget.photos.isNotEmpty()) {
                showGifReplaceDialog = true
            } else {
                onPickFromSourceClick()
            }
        },
        onPhotoClick = viewModel::previewPhoto,
        onReorderFinish = viewModel::reorderPhotos,
        onRemovedPhotoClick = { photo ->
            recentlyDeletedPhotoSheetState.showBottomSheet(data = photo)
        },
        onSetPhotoTime = { photo, time -> viewModel.setPhotoScheduledTime(photo.photoId, time) },
        onClearPhotoTime = { photo -> viewModel.clearPhotoScheduledTime(photo.photoId) },
        onEvenSplitClick = viewModel::applyEvenSplitSchedule,
        modifier = modifier,
    )

    if (showGifReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showGifReplaceDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showGifReplaceDialog = false
                        onPickFromSourceClick()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = R.string.photo_widget_action_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGifReplaceDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = R.string.photo_widget_action_cancel))
                }
            },
            text = {
                Text(text = stringResource(id = R.string.photo_widget_configure_pick_gif_replace))
            },
        )
    }

    // region Sheets
    PhotoWidgetSourceBottomSheet(
        sheetState = sourceSheetState,
        currentSource = state.photoWidget.source,
        syncedDir = state.photoWidget.syncedDir,
        onDirRemove = viewModel::removeDir,
        onChangeSource = viewModel::changeSource,
    )

    ImportFromWidgetBottomSheet(
        sheetState = importFromWidgetSheetState,
        onWidgetSelect = viewModel::importFromWidget,
    )

    RecentlyDeletedPhotoBottomSheet(
        sheetState = recentlyDeletedPhotoSheetState,
        onRestore = viewModel::restorePhoto,
        onDelete = viewModel::deletePhotoPermanently,
    )
    // endregion Sheets
}

@Composable
fun PhotoWidgetConfigureContentTab(
    photoWidget: PhotoWidget,
    onChangeSourceClick: () -> Unit,
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
    onPickFromSourceClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinish: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
    onSetPhotoTime: (LocalPhoto, Time) -> Unit = { _, _ -> },
    onClearPhotoTime: (LocalPhoto) -> Unit = {},
    onEvenSplitClick: () -> Unit = {},
) {
    PhotoPicker(
        source = photoWidget.source,
        onChangeSourceClick = onChangeSourceClick,
        isImportAvailable = isImportAvailable,
        onImportClick = onImportClick,
        photos = remember(photoWidget.photos, photoWidget.cycleMode) { photoWidget.orderedPhotosForDisplay() },
        canSort = photoWidget.canSort,
        cycleMode = photoWidget.cycleMode,
        onPickFromSourceClick = onPickFromSourceClick,
        onPhotoClick = onPhotoClick,
        onReorderFinish = onReorderFinish,
        removedPhotos = photoWidget.removedPhotos,
        onRemovedPhotoClick = onRemovedPhotoClick,
        onSetPhotoTime = onSetPhotoTime,
        onClearPhotoTime = onClearPhotoTime,
        onEvenSplitClick = onEvenSplitClick,
        aspectRatio = photoWidget.aspectRatio,
        shapeId = photoWidget.shapeId,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun PhotoPicker(
    source: PhotoWidgetSource,
    onChangeSourceClick: () -> Unit,
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
    photos: List<LocalPhoto>,
    canSort: Boolean,
    cycleMode: PhotoWidgetCycleMode,
    onPickFromSourceClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinish: (List<LocalPhoto>) -> Unit,
    removedPhotos: List<LocalPhoto>,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onSetPhotoTime: (LocalPhoto, Time) -> Unit,
    onClearPhotoTime: (LocalPhoto) -> Unit,
    onEvenSplitClick: () -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    val isAdvancedSchedule: Boolean = cycleMode is PhotoWidgetCycleMode.AdvancedSchedule
    val scheduleMap: Map<String, Time> = (cycleMode as? PhotoWidgetCycleMode.AdvancedSchedule)?.schedule.orEmpty()
    var timePickerPhoto: LocalPhoto? by rememberSaveable { mutableStateOf(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        PhotoGrid(
            photos = photos,
            source = source,
            aspectRatio = aspectRatio,
            shapeId = shapeId,
            canReorder = canSort,
            onReorderFinish = onReorderFinish,
            onPhotoClick = onPhotoClick,
            isAdvancedSchedule = isAdvancedSchedule,
            scheduleMap = scheduleMap,
            onTimeClick = { photo -> timePickerPhoto = photo },
            onTimeLongClick = onClearPhotoTime,
            contentPadding = PaddingValues(start = 16.dp, top = 68.dp, end = 16.dp, bottom = 240.dp),
        )

        PhotoPickerHeader(
            pickerButtonText = stringResource(
                id = when (source) {
                    PhotoWidgetSource.PHOTOS -> R.string.photo_widget_configure_pick_photo
                    PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_configure_pick_folder
                    PhotoWidgetSource.GIF -> R.string.photo_widget_configure_pick_gif
                },
            ),
            onPickFromSourceClick = onPickFromSourceClick,
            onChangeSourceClick = onChangeSourceClick,
            showImportPrompt = isImportAvailable && photos.isEmpty() && removedPhotos.isEmpty(),
            onImportClick = onImportClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        PhotoPickerFooter(
            showAdvancedScheduleWarning = isAdvancedSchedule && photos.any { it.photoId !in scheduleMap },
            onEvenSplitClick = onEvenSplitClick,
            removedPhotos = removedPhotos,
            source = source,
            onRemovedPhotoClick = onRemovedPhotoClick,
            aspectRatio = aspectRatio,
            shapeId = shapeId,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (isAdvancedSchedule && timePickerPhoto != null) {
        TimePickerDialog(
            onConfirm = { state: TimePickerState ->
                timePickerPhoto?.let { photo ->
                    onSetPhotoTime(photo, Time(hour = state.hour, minute = state.minute))
                }
                timePickerPhoto = null
            },
            onDismiss = { timePickerPhoto = null },
        )
    }
}

@Composable
private fun PhotoPickerHeader(
    pickerButtonText: String,
    onPickFromSourceClick: () -> Unit,
    onChangeSourceClick: () -> Unit,
    showImportPrompt: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to MaterialTheme.colorScheme.background,
                        0.8f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            .padding(all = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val interactionSources: Array<MutableInteractionSource> = remember {
                Array(size = 2) { MutableInteractionSource() }
            }

            OutlinedButton(
                onClick = onPickFromSourceClick,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                interactionSource = interactionSources[0],
            ) {
                AutoSizeText(
                    text = pickerButtonText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            OutlinedButton(
                onClick = onChangeSourceClick,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                interactionSource = interactionSources[1],
            ) {
                AutoSizeText(
                    text = stringResource(R.string.photo_widget_configure_change_source),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        AnimatedVisibility(
            visible = showImportPrompt,
            exit = fadeOut() + slideOutVertically(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.photo_widget_configure_import_prompt),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                )

                TextButton(
                    onClick = onImportClick,
                ) {
                    Text(
                        text = stringResource(R.string.photo_widget_configure_import_prompt_action),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<LocalPhoto>,
    source: PhotoWidgetSource,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    canReorder: Boolean,
    onReorderFinish: (List<LocalPhoto>) -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    isAdvancedSchedule: Boolean,
    scheduleMap: Map<String, Time>,
    onTimeClick: (LocalPhoto) -> Unit,
    onTimeLongClick: (LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val localHaptics: HapticFeedback = LocalHapticFeedback.current

    val currentPhotos: SnapshotStateList<LocalPhoto> = remember { photos.toMutableStateList() }
    SideEffect(photos) {
        if (currentPhotos != photos) {
            currentPhotos.clear()
            currentPhotos.addAll(photos)
        }
    }

    val lazyGridState: LazyGridState = rememberLazyGridState()
    val reorderableLazyGridState: ReorderableLazyGridState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
    ) { from: LazyGridItemInfo, to: LazyGridItemInfo ->
        currentPhotos.apply {
            this[to.index - 1] = this[from.index - 1].also {
                this[from.index - 1] = this[to.index - 1]
            }
        }
        localHaptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(count = 4),
        modifier = modifier
            .fillMaxSize()
            .fadingEdges(scrollState = lazyGridState),
        state = lazyGridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "photo-quantity", span = { GridItemSpan(4) }) {
            Text(
                text = pluralStringResource(
                    if (source == PhotoWidgetSource.GIF) {
                        R.plurals.photo_widget_configure_photo_quantity_gif
                    } else {
                        R.plurals.photo_widget_configure_photo_quantity
                    },
                    currentPhotos.size,
                    currentPhotos.size,
                ),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        items(currentPhotos, key = { photo -> photo }) { photo ->
            ReorderableItem(reorderableLazyGridState, key = photo) {
                Box {
                    ShapedPhoto(
                        photo = photo,
                        aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                        shapeId = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                            shapeId
                        } else {
                            PhotoWidget.DEFAULT_SHAPE_ID
                        },
                        cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                        modifier = Modifier
                            .longPressDraggableHandle(
                                enabled = canReorder,
                                onDragStarted = {
                                    localHaptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    onReorderFinish(currentPhotos)
                                    localHaptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                },
                            )
                            .aspectRatio(ratio = 1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Image,
                                onClick = { onPhotoClick(photo) },
                            ),
                    )

                    if (isAdvancedSchedule) {
                        val timeText: String = scheduleMap[photo.photoId]?.asString()
                            ?: stringResource(R.string.photo_widget_configure_cycle_mode_advanced_schedule_set_time)
                        AutoSizeText(
                            text = timeText,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(all = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .combinedClickable(
                                    onClick = { onTimeClick(photo) },
                                    onLongClick = { onTimeLongClick(photo) },
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPickerFooter(
    showAdvancedScheduleWarning: Boolean,
    onEvenSplitClick: () -> Unit,
    removedPhotos: List<LocalPhoto>,
    source: PhotoWidgetSource,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        AnimatedVisibility(
            visible = showAdvancedScheduleWarning,
            modifier = Modifier.fillMaxWidth(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            InformationalPanel(
                text = stringResource(R.string.photo_widget_configure_cycle_mode_advanced_schedule_no_time_warning),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                showActionButton = true,
                actionButtonText = stringResource(
                    R.string.photo_widget_configure_cycle_mode_advanced_schedule_even_split,
                ),
                onActionButtonClick = onEvenSplitClick,
            )
        }

        AnimatedVisibility(
            visible = removedPhotos.isNotEmpty() && source != PhotoWidgetSource.GIF,
            modifier = Modifier.fillMaxWidth(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            RemovedPhotosPicker(
                title = when (source) {
                    PhotoWidgetSource.DIRECTORY -> stringResource(R.string.photo_widget_configure_photos_excluded)
                    else -> stringResource(R.string.photo_widget_configure_photos_pending_deletion)
                },
                photos = removedPhotos,
                onPhotoClick = onRemovedPhotoClick,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.2f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                1f to MaterialTheme.colorScheme.background,
                            ),
                        ),
                    )
                    .padding(top = 32.dp),
            )
        }

        AnimatedVisibility(
            visible = source == PhotoWidgetSource.GIF,
            modifier = Modifier.fillMaxWidth(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            InformationalPanel(
                text = stringResource(R.string.warning_gif_widget_battery_usage),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun RemovedPhotosPicker(
    title: String,
    photos: List<LocalPhoto>,
    onPhotoClick: (LocalPhoto) -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(photos, key = { it.photoId }) { photo ->
                ShapedPhoto(
                    photo = photo,
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    shapeId = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                        shapeId
                    } else {
                        PhotoWidget.DEFAULT_SHAPE_ID
                    },
                    cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .aspectRatio(ratio = 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Image,
                            onClick = { onPhotoClick(photo) },
                        ),
                    colors = PhotoWidgetColors(saturation = 0f),
                )
            }
        }
    }
}

// region Previews
@PreviewAll
@Composable
private fun PhotoWidgetConfigureContentTabPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureContentTab(
            photoWidget = PhotoWidget(
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
            ),
            onChangeSourceClick = {},
            isImportAvailable = false,
            onImportClick = {},
            onPickFromSourceClick = {},
            onPhotoClick = {},
            onReorderFinish = {},
            onRemovedPhotoClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@PreviewAll
@Composable
private fun PhotoWidgetConfigureContentTabDirectoryPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureContentTab(
            photoWidget = PhotoWidget(
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
                source = PhotoWidgetSource.DIRECTORY,
            ),
            onChangeSourceClick = {},
            isImportAvailable = false,
            onImportClick = {},
            onPickFromSourceClick = {},
            onPhotoClick = {},
            onReorderFinish = {},
            onRemovedPhotoClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@PreviewAll
@Composable
private fun PhotoWidgetConfigureContentTabAdvancedSchedulePreview() {
    ExtendedTheme {
        PhotoWidgetConfigureContentTab(
            photoWidget = PhotoWidget(
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
                cycleMode = PhotoWidgetCycleMode.AdvancedSchedule(),
            ),
            onChangeSourceClick = {},
            isImportAvailable = false,
            onImportClick = {},
            onPickFromSourceClick = {},
            onPhotoClick = {},
            onReorderFinish = {},
            onRemovedPhotoClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
// endregion Previews
