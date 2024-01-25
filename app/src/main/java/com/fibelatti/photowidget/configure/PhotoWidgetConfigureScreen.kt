package com.fibelatti.photowidget.configure

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoWidgetConfigureScreen(
    photos: List<LocalPhoto>,
    selectedPhoto: LocalPhoto?,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    loopingInterval: PhotoWidgetLoopingInterval,
    onLoopingIntervalPickerClick: () -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    onShapeClick: (String) -> Unit,
    onAddToHomeClick: () -> Unit,
    isProcessing: Boolean,
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
                photos = photos,
                selectedPhoto = selectedPhoto,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                onMoveLeftClick = onMoveLeftClick,
                onMoveRightClick = onMoveRightClick,
                onAspectRatioClick = onAspectRatioClick,
                onCropClick = onCropClick,
                onRemoveClick = onRemoveClick,
                onPhotoPickerClick = onPhotoPickerClick,
                onPhotoClick = onPhotoClick,
                loopingInterval = loopingInterval,
                onLoopingIntervalPickerClick = onLoopingIntervalPickerClick,
                onShapeClick = onShapeClick,
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
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "LoadingIndicator_Transition")

        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 180f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_000),
                repeatMode = RepeatMode.Restart,
            ),
            label = "LoadingIndicator_Rotation",
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_hourglass),
            contentDescription = "",
            modifier = Modifier
                .size(72.dp)
                .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                .padding(all = 16.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun PhotoWidgetConfigureContent(
    photos: List<LocalPhoto>,
    selectedPhoto: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    loopingInterval: PhotoWidgetLoopingInterval,
    onLoopingIntervalPickerClick: () -> Unit,
    onShapeClick: (String) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            PhotoWidgetViewer(
                photo = selectedPhoto,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                modifier = Modifier.fillMaxSize(),
            )

            FilledTonalIconButton(
                onClick = onAspectRatioClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(all = 8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_aspect_ratio),
                    contentDescription = stringResource(id = R.string.photo_widget_aspect_ratio_title),
                )
            }

            if (selectedPhoto != null) {
                EditingControls(
                    onCropClick = { onCropClick(selectedPhoto) },
                    onRemoveClick = { onRemoveClick(selectedPhoto) },
                    showMoveControls = photos.size > 1,
                    moveLeftEnabled = photos.indexOf(selectedPhoto) != 0,
                    onMoveLeftClick = { onMoveLeftClick(selectedPhoto) },
                    moveRightEnabled = photos.indexOf(selectedPhoto) < photos.size - 1,
                    onMoveRightClick = { onMoveRightClick(selectedPhoto) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }

        PhotoPicker(
            photos = photos,
            onPhotoPickerClick = onPhotoPickerClick,
            onPhotoClick = onPhotoClick,
            aspectRatio = aspectRatio,
            shapeId = shapeId,
            modifier = Modifier.padding(top = 16.dp),
        )

        AnimatedVisibility(visible = photos.size > 1) {
            PhotoIntervalPicker(
                loopingInterval = loopingInterval,
                onLoopingIntervalPickerClick = onLoopingIntervalPickerClick,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        AnimatedVisibility(visible = aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
            ShapePicker(
                shapeId = shapeId,
                onShapeClick = onShapeClick,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        FilledTonalButton(
            onClick = onAddToHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 32.dp, end = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_configure_add_to_home))
        }

        Spacer(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun PhotoWidgetViewer(
    photo: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
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
                modifier = Modifier
                    .padding(all = 32.dp)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EditingControls(
    onCropClick: () -> Unit,
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

        IconButton(onClick = onRemoveClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = stringResource(id = R.string.photo_widget_configure_menu_remove),
            )
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

@Composable
private fun PhotoPicker(
    photos: List<LocalPhoto>,
    onPhotoPickerClick: () -> Unit,
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
            text = stringResource(
                id = if (photos.isEmpty()) {
                    R.string.photo_widget_configure_select_photo
                } else {
                    R.string.photo_widget_configure_selected_photos
                },
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val shape = remember(shapeId) {
                    PhotoWidgetShapeBuilder.buildShape(
                        shapeId = shapeId,
                        width = 1,
                        height = 1,
                    )
                }

                ColoredShape(
                    polygon = shape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio = 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPhotoPickerClick,
                            role = Role.Button,
                        ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pick_image),
                        contentDescription = stringResource(id = R.string.photo_widget_cd_open_photo_picker),
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
                    modifier = Modifier
                        .fillMaxHeight()
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
    onLoopingIntervalPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_select_interval),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedButton(
            onClick = onLoopingIntervalPickerClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = loopingInterval.title))
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.photo_widget_configure_applied_shape),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )

        val shapesToPolygons = remember {
            PhotoWidgetShapeBuilder.buildAllShapes(
                width = 1,
                height = 1,
            ).toList()
        }

        val state = rememberLazyListState()

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            state = state,
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

        LaunchedEffect(shapeId) {
            val index = shapesToPolygons.indexOfFirst { it.first.id == shapeId }
                .coerceAtLeast(minimumValue = 0)

            launch {
                state.animateScrollToItem(index)
            }
        }
    }
}

@Composable
fun ShapedPhoto(
    photo: LocalPhoto,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {},
) {
    val photoBitmap = remember(photo) {
        BitmapFactory.decodeFile(photo.path)
    }
    val transformedBitmap = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
        remember(photo, shapeId) {
            val shape = PhotoWidgetShapeBuilder.buildShape(
                shapeId = shapeId,
                width = photoBitmap.width,
                height = photoBitmap.height,
            )

            photoBitmap.withPolygonalShape(roundedPolygon = shape).asImageBitmap()
        }
    } else {
        remember(photo, aspectRatio) {
            photoBitmap.withRoundedCorners(desiredAspectRatio = aspectRatio).asImageBitmap()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = transformedBitmap,
            contentDescription = "",
            modifier = Modifier
                .aspectRatio(ratio = aspectRatio.aspectRatio)
                .fillMaxSize(),
            contentScale = ContentScale.FillWidth,
        )

        badge()
    }
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
                width = size.width.toInt(),
                height = size.height.toInt(),
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
private fun PhotoWidgetConfigureScreenPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photos = emptyList(),
            selectedPhoto = null,
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onPhotoPickerClick = {},
            onPhotoClick = {},
            loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
            onLoopingIntervalPickerClick = {},
            aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            shapeId = PhotoWidgetShapeBuilder.defaultShapeId(),
            onShapeClick = {},
            onAddToHomeClick = {},
            isProcessing = false,
        )
    }
}
