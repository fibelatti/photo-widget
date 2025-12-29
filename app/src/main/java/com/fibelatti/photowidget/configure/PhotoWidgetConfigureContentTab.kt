@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.canSort
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.text.AutoSizeText
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun PhotoWidgetConfigureContentTab(
    photoWidget: PhotoWidget,
    onChangeSourceClick: () -> Unit,
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoPicker(
        source = photoWidget.source,
        onChangeSourceClick = onChangeSourceClick,
        isImportAvailable = isImportAvailable,
        onImportClick = onImportClick,
        photos = photoWidget.photos,
        canSort = photoWidget.canSort,
        onPhotoPickerClick = onPhotoPickerClick,
        onDirPickerClick = onDirPickerClick,
        onPhotoClick = onPhotoClick,
        onReorderFinished = onReorderFinished,
        removedPhotos = photoWidget.removedPhotos,
        onRemovedPhotoClick = onRemovedPhotoClick,
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
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    removedPhotos: List<LocalPhoto>,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val localHaptics = LocalHapticFeedback.current

        val currentPhotos by rememberUpdatedState(photos.toMutableStateList())
        val lazyGridState = rememberLazyGridState()
        val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
            currentPhotos.apply {
                this[to.index] = this[from.index].also {
                    this[from.index] = this[to.index]
                }
            }
            localHaptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(count = 5),
            modifier = Modifier
                .fillMaxSize()
                .fadingEdges(scrollState = lazyGridState),
            state = lazyGridState,
            contentPadding = PaddingValues(start = 16.dp, top = 68.dp, end = 16.dp, bottom = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(currentPhotos, key = { photo -> photo }) { photo ->
                ReorderableItem(reorderableLazyGridState, key = photo) {
                    ShapedPhoto(
                        photo = photo,
                        aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                        shapeId = if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                            shapeId
                        } else {
                            PhotoWidget.DEFAULT_SHAPE_ID
                        },
                        cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                        modifier = Modifier
                            .animateItem()
                            .longPressDraggableHandle(
                                enabled = canSort,
                                onDragStarted = {
                                    localHaptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    onReorderFinished(currentPhotos)
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
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
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
                    onClick = {
                        when (source) {
                            PhotoWidgetSource.PHOTOS -> onPhotoPickerClick()
                            PhotoWidgetSource.DIRECTORY -> onDirPickerClick()
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    interactionSource = interactionSources[0],
                ) {
                    AutoSizeText(
                        text = stringResource(
                            id = when (source) {
                                PhotoWidgetSource.PHOTOS -> R.string.photo_widget_configure_pick_photo
                                PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_configure_pick_folder
                            },
                        ),
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
                visible = isImportAvailable && photos.isEmpty() && removedPhotos.isEmpty(),
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

        AnimatedVisibility(
            visible = removedPhotos.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RemovedPhotosPicker(
                title = when (source) {
                    PhotoWidgetSource.PHOTOS -> stringResource(
                        R.string.photo_widget_configure_photos_pending_deletion,
                    )

                    PhotoWidgetSource.DIRECTORY -> stringResource(R.string.photo_widget_configure_photos_excluded)
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
                    shapeId = if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
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
