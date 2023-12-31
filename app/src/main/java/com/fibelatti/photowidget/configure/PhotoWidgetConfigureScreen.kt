package com.fibelatti.photowidget.configure

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.foundation.StableList
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoWidgetConfigureScreen(
    photos: StableList<String>,
    onPhotoPickerClick: () -> Unit,
    onPhotoLongClick: (String) -> Unit,
    loopingInterval: PhotoWidgetLoopingInterval,
    onLoopingIntervalPickerClick: () -> Unit,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    onShapeClick: (String) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            val firstPhotoPath = photos.value.firstOrNull()
            var selectedPhotoPath: String? by remember(firstPhotoPath) {
                mutableStateOf(firstPhotoPath)
            }

            PhotoWidgetViewer(
                photoPath = selectedPhotoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            PhotoPicker(
                photos = photos,
                onPhotoPickerClick = onPhotoPickerClick,
                onPhotoClick = { photoPath -> selectedPhotoPath = photoPath },
                onPhotoLongClick = onPhotoLongClick,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                modifier = Modifier.padding(top = 16.dp),
            )

            AnimatedVisibility(visible = photos.value.size > 1) {
                PhotoIntervalPicker(
                    loopingInterval = loopingInterval,
                    onLoopingIntervalPickerClick = onLoopingIntervalPickerClick,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                ShapePicker(
                    shapeId = shapeId,
                    onShapeClick = onShapeClick,
                    modifier = modifier.padding(top = 16.dp),
                )
            }

            FilledTonalButton(
                onClick = onAddToHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
            ) {
                Text(text = stringResource(id = R.string.photo_widget_configure_add_to_home))
            }
        }
    }
}

@Composable
private fun PhotoWidgetViewer(
    photoPath: String?,
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

        if (photoPath != null) {
            ShapedPhoto(
                photoPath = photoPath,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 32.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PhotoPicker(
    photos: StableList<String>,
    onPhotoPickerClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onPhotoLongClick: (String) -> Unit,
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
                id = if (photos.value.isEmpty()) {
                    R.string.photo_widget_configure_select_photo
                } else {
                    R.string.photo_widget_configure_selected_photos
                },
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )

        val haptic = LocalHapticFeedback.current
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stickyHeader {
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

            items(photos.value) { photo ->
                ShapedPhoto(
                    photoPath = photo,
                    aspectRatio = aspectRatio,
                    shapeId = shapeId,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(ratio = aspectRatio.aspectRatio)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Image,
                            onClick = { onPhotoClick(photo) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPhotoLongClick(photo)
                            },
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
private fun ShapedPhoto(
    photoPath: String,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    modifier: Modifier = Modifier,
) {
    val photoBitmap = remember(photoPath) {
        BitmapFactory.decodeFile(photoPath)
    }
    val transformedBitmap = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
        remember(photoPath, shapeId) {
            val shape = PhotoWidgetShapeBuilder.buildShape(
                shapeId = shapeId,
                width = photoBitmap.width,
                height = photoBitmap.height,
            )

            photoBitmap.withPolygonalShape(shape).asImageBitmap()
        }
    } else {
        remember(photoPath, aspectRatio) {
            photoBitmap.withRoundedCorners().asImageBitmap()
        }
    }

    Image(
        bitmap = transformedBitmap,
        contentDescription = "",
        modifier = modifier,
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
            photos = StableList(),
            onPhotoPickerClick = {},
            onPhotoLongClick = {},
            loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
            onLoopingIntervalPickerClick = {},
            aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            shapeId = PhotoWidgetShapeBuilder.defaultShapeId(),
            onShapeClick = {},
            onAddToHomeClick = {},
        )
    }
}
