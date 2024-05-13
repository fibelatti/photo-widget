package com.fibelatti.photowidget.configure

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit

@Composable
fun PhotoWidgetConfigureScreen(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    isProcessing: Boolean,
    onNavClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onShuffleClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onLoopingIntervalPickerClick: (PhotoWidgetLoopingInterval, intervalBasedLoopingEnabled: Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction, appShortcut: String?, increaseBrightness: Boolean) -> Unit,
    onShapeClick: (String) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.Center,
        ) {
            val blurRadius by animateDpAsState(
                targetValue = if (isProcessing) 10.dp else 0.dp,
                label = "ProcessingBlur",
            )

            PhotoWidgetConfigureContent(
                photoWidget = photoWidget,
                selectedPhoto = selectedPhoto,
                onNavClick = onNavClick,
                onMoveLeftClick = onMoveLeftClick,
                onMoveRightClick = onMoveRightClick,
                onAspectRatioClick = onAspectRatioClick,
                onCropClick = onCropClick,
                onRemoveClick = onRemoveClick,
                onChangeSource = onChangeSource,
                onShuffleClick = onShuffleClick,
                onPhotoPickerClick = onPhotoPickerClick,
                onDirPickerClick = onDirPickerClick,
                onPhotoClick = onPhotoClick,
                onLoopingIntervalPickerClick = onLoopingIntervalPickerClick,
                onTapActionPickerClick = onTapActionPickerClick,
                onShapeClick = onShapeClick,
                onCornerRadiusChange = onCornerRadiusChange,
                onAddToHomeClick = onAddToHomeClick,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = blurRadius)
                    .padding(paddingValues),
            )

            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoWidgetConfigureContent(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    onNavClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onShuffleClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onLoopingIntervalPickerClick: (PhotoWidgetLoopingInterval, intervalBasedLoopingEnabled: Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction, appShortcut: String?, increaseBrightness: Boolean) -> Unit,
    onShapeClick: (String) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            PhotoWidgetViewer(
                photo = selectedPhoto,
                aspectRatio = photoWidget.aspectRatio,
                shapeId = photoWidget.shapeId,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = photoWidget.cornerRadius,
            )

            IconButton(
                onClick = onNavClick,
                modifier = Modifier.align(Alignment.TopStart),
                colors = IconButtonDefaults.iconButtonColors().copy(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                )
            }

            ConfigurationControl(
                label = photoWidget.aspectRatio.label,
                icon = R.drawable.ic_aspect_ratio,
                contentDescription = R.string.photo_widget_aspect_ratio_title,
                onClick = onAspectRatioClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 8.dp),
            )

            if (selectedPhoto != null) {
                EditingControls(
                    onCropClick = { onCropClick(selectedPhoto) },
                    showRemove = PhotoWidgetSource.PHOTOS == photoWidget.source,
                    onRemoveClick = { onRemoveClick(selectedPhoto) },
                    showMoveControls = photoWidget.canSort,
                    moveLeftEnabled = photoWidget.photos.indexOf(selectedPhoto) != 0,
                    onMoveLeftClick = { onMoveLeftClick(selectedPhoto) },
                    moveRightEnabled = photoWidget.photos.indexOf(selectedPhoto) < photoWidget.photos.size - 1,
                    onMoveRightClick = { onMoveRightClick(selectedPhoto) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }

        PhotoPicker(
            source = photoWidget.source,
            onChangeSource = { onChangeSource(photoWidget.source, photoWidget.syncedDir) },
            photos = photoWidget.photos,
            shuffleVisible = photoWidget.canShuffle,
            shuffle = photoWidget.shuffle,
            onShuffleClick = onShuffleClick,
            onPhotoPickerClick = onPhotoPickerClick,
            onDirPickerClick = onDirPickerClick,
            onPhotoClick = onPhotoClick,
            aspectRatio = photoWidget.aspectRatio,
            shapeId = photoWidget.shapeId,
            cornerRadius = photoWidget.cornerRadius,
        )

        AnimatedContent(
            targetState = photoWidget.aspectRatio,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Customization_Picker",
        ) { aspectRatio ->
            if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                ShapePicker(
                    shapeId = photoWidget.shapeId,
                    onShapeClick = onShapeClick,
                )
            } else {
                CornerRadiusPicker(
                    value = photoWidget.cornerRadius,
                    onValueChange = onCornerRadiusChange,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TapActionPicker(
                tapAction = photoWidget.tapAction,
                onTapActionPickerClick = {
                    onTapActionPickerClick(
                        photoWidget.tapAction,
                        photoWidget.appShortcut,
                        photoWidget.increaseBrightness,
                    )
                },
                modifier = Modifier.weight(1f),
            )

            if (photoWidget.photos.size > 1) {
                PhotoIntervalPicker(
                    loopingInterval = photoWidget.loopingInterval,
                    intervalBasedLoopingEnabled = photoWidget.intervalBasedLoopingEnabled,
                    onLoopingIntervalPickerClick = {
                        onLoopingIntervalPickerClick(
                            photoWidget.loopingInterval,
                            photoWidget.intervalBasedLoopingEnabled,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FilledTonalButton(
            onClick = onAddToHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_configure_add_to_home))
        }
    }
}

@Composable
private fun PhotoWidgetViewer(
    photo: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val colors = listOf(
            Color.White,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        )

        val largeRadialGradient = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = RadialGradientShader(
                colors = colors,
                center = size.center,
                radius = maxOf(size.height, size.width),
                colorStops = listOf(0f, 0.9f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(largeRadialGradient)
                .blur(10.dp),
        )

        if (photo != null) {
            ShapedPhoto(
                photo = photo,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 48.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ConfigurationControl(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(size = 24.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = label),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )

        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(id = contentDescription),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun EditingControls(
    onCropClick: () -> Unit,
    showRemove: Boolean,
    onRemoveClick: () -> Unit,
    showMoveControls: Boolean,
    moveLeftEnabled: Boolean,
    onMoveLeftClick: () -> Unit,
    moveRightEnabled: Boolean,
    onMoveRightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(size = 24.dp),
            )
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMoveControls) {
            IconButton(
                onClick = onMoveLeftClick,
                enabled = moveLeftEnabled,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_left),
                )
            }
        }

        IconButton(onClick = onCropClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_crop),
                contentDescription = stringResource(id = R.string.photo_widget_configure_menu_crop),
            )
        }

        if (showRemove) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash),
                    contentDescription = stringResource(id = R.string.photo_widget_configure_menu_remove),
                )
            }
        }

        if (showMoveControls) {
            IconButton(
                onClick = onMoveRightClick,
                enabled = moveRightEnabled,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_right),
                )
            }
        }
    }
}

// region Pickers
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PhotoPicker(
    source: PhotoWidgetSource,
    onChangeSource: () -> Unit,
    photos: List<LocalPhoto>,
    shuffleVisible: Boolean,
    shuffle: Boolean,
    onShuffleClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    id = when (source) {
                        PhotoWidgetSource.PHOTOS -> R.string.photo_widget_configure_photos
                        PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_configure_photos_from_dir
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                style = MaterialTheme.typography.titleMedium,
            )

            AnimatedVisibility(
                visible = shuffleVisible,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.alignByBaseline(),
            ) {
                ConfigurationControl(
                    label = if (shuffle) {
                        R.string.photo_widget_configure_shuffle_on
                    } else {
                        R.string.photo_widget_configure_shuffle_off
                    },
                    icon = R.drawable.ic_shuffle,
                    contentDescription = R.string.photo_widget_cd_toggle_shuffle,
                    onClick = onShuffleClick,
                )
            }

            ConfigurationControl(
                label = R.string.photo_widget_configure_menu_source,
                icon = R.drawable.ic_pick_folder,
                contentDescription = R.string.photo_widget_cd_change_source,
                onClick = onChangeSource,
                modifier = Modifier.alignByBaseline(),
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp),
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stickyHeader {
                ColoredShape(
                    polygon = remember(shapeId) {
                        PhotoWidgetShapeBuilder.buildShape(shapeId = shapeId)
                    },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio = 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                when (source) {
                                    PhotoWidgetSource.PHOTOS -> onPhotoPickerClick()
                                    PhotoWidgetSource.DIRECTORY -> onDirPickerClick()
                                }
                            },
                            role = Role.Button,
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to MaterialTheme.colorScheme.background,
                                    0.9f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                    1f to Color.Transparent,
                                ),
                            ),
                        ),
                ) {
                    Icon(
                        painter = painterResource(
                            id = when (source) {
                                PhotoWidgetSource.PHOTOS -> R.drawable.ic_pick_image
                                PhotoWidgetSource.DIRECTORY -> R.drawable.ic_pick_folder
                            },
                        ),
                        contentDescription = stringResource(
                            id = when (source) {
                                PhotoWidgetSource.PHOTOS -> R.string.photo_widget_cd_open_photo_picker
                                PhotoWidgetSource.DIRECTORY -> R.string.photo_widget_cd_open_folder_picker
                            },
                        ),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            items(photos) { photo ->
                ShapedPhoto(
                    photo = photo,
                    aspectRatio = aspectRatio,
                    shapeId = shapeId,
                    cornerRadius = cornerRadius,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio = aspectRatio.aspectRatio)
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
}

@Composable
private fun PhotoIntervalPicker(
    loopingInterval: PhotoWidgetLoopingInterval,
    intervalBasedLoopingEnabled: Boolean,
    onLoopingIntervalPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_select_interval),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedButton(
            onClick = onLoopingIntervalPickerClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val intervalString = if (TimeUnit.HOURS == loopingInterval.timeUnit) {
                pluralStringResource(
                    id = R.plurals.photo_widget_configure_interval_current_hours,
                    count = loopingInterval.repeatInterval.toInt(),
                    loopingInterval.repeatInterval,
                )
            } else {
                pluralStringResource(
                    id = R.plurals.photo_widget_configure_interval_current_minutes,
                    count = loopingInterval.repeatInterval.toInt(),
                    loopingInterval.repeatInterval,
                )
            }

            AutoSizeText(
                text = if (intervalBasedLoopingEnabled) {
                    stringResource(id = R.string.photo_widget_configure_interval_current_label, intervalString)
                } else {
                    stringResource(id = R.string.photo_widget_configure_interval_current_disabled)
                },
                maxLines = 1,
                minTextSize = 8.sp,
            )
        }
    }
}

@Composable
private fun TapActionPicker(
    tapAction: PhotoWidgetTapAction,
    onTapActionPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_tap_action),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedButton(
            onClick = onTapActionPickerClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AutoSizeText(
                text = stringResource(id = tapAction.label),
                maxLines = 1,
                minTextSize = 8.sp,
            )
        }
    }
}

@Composable
private fun ShapePicker(
    shapeId: String,
    onShapeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_applied_shape),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )

        val shapesToPolygons = remember {
            PhotoWidgetShapeBuilder.buildAllShapes().toList()
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shapesToPolygons) { (shape, polygon) ->
                val color by animateColorAsState(
                    targetValue = if (shape.id == shapeId) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "ShapePicker_SelectedColor",
                )

                ColoredShape(
                    polygon = polygon,
                    color = color,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio = 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                        ) {
                            onShapeClick(shape.id)
                        },
                )
            }
        }
    }
}

@Composable
private fun CornerRadiusPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_corner_radius),
            style = MaterialTheme.typography.titleMedium,
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(horizontal = 4.dp),
            valueRange = 0f..100f,
        )
    }
}
// endregion Pickers

@Composable
fun ShapedPhoto(
    photo: LocalPhoto,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {},
) {
    AsyncPhotoViewer(
        data = when {
            !photo.path.isNullOrEmpty() -> photo.path
            photo.externalUri != null -> photo.externalUri
            else -> null
        },
        dataKey = arrayOf(photo, shapeId, aspectRatio, cornerRadius),
        contentScale = if (PhotoWidgetAspectRatio.ORIGINAL != aspectRatio) {
            ContentScale.FillWidth
        } else {
            ContentScale.Inside
        },
        modifier = modifier.aspectRatio(ratio = aspectRatio.aspectRatio),
        transformer = { bitmap ->
            bitmap?.run {
                if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                    withPolygonalShape(shapeId = shapeId)
                } else {
                    withRoundedCorners(
                        desiredAspectRatio = aspectRatio,
                        radius = cornerRadius,
                    )
                }
            }
        },
        badge = badge,
    )
}

@Composable
fun ColoredShape(
    polygon: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.drawWithContent {
            val sizedPolygon = PhotoWidgetShapeBuilder.resizeShape(
                roundedPolygon = polygon,
                width = size.width,
                height = size.height,
            )

            drawPath(
                path = sizedPolygon
                    .toPath()
                    .asComposePath(),
                color = color,
            )

            drawContent()
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun PhotoWidgetConfigureScreenPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                source = PhotoWidgetSource.PHOTOS,
                photos = listOf(
                    LocalPhoto(name = "photo-1"),
                    LocalPhoto(name = "photo-2"),
                ),
                shuffle = false,
                loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
                tapAction = PhotoWidgetTapAction.VIEW_FULL_SCREEN,
                aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                shapeId = PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
                cornerRadius = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
            ),
            selectedPhoto = LocalPhoto(name = "photo-1"),
            isProcessing = false,
            onNavClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onChangeSource = { _, _ -> },
            onShuffleClick = {},
            onPhotoPickerClick = {},
            onDirPickerClick = {},
            onPhotoClick = {},
            onLoopingIntervalPickerClick = { _, _ -> },
            onTapActionPickerClick = { _, _, _ -> },
            onShapeClick = {},
            onCornerRadiusChange = {},
            onAddToHomeClick = {},
        )
    }
}
