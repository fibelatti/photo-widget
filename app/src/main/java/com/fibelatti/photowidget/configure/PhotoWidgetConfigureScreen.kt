package com.fibelatti.photowidget.configure

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.foundation.conditional
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit

@Composable
fun PhotoWidgetConfigureScreen(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onLoopingIntervalPickerClick: (PhotoWidgetLoopingInterval, intervalBasedLoopingEnabled: Boolean) -> Unit,
    onTapActionPickerClick: () -> Unit,
    onShapeClick: (String) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
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
                photoWidget = photoWidget,
                selectedPhoto = selectedPhoto,
                onMoveLeftClick = onMoveLeftClick,
                onMoveRightClick = onMoveRightClick,
                onAspectRatioClick = onAspectRatioClick,
                onCropClick = onCropClick,
                onRemoveClick = onRemoveClick,
                onPhotoPickerClick = onPhotoPickerClick,
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
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onLoopingIntervalPickerClick: (PhotoWidgetLoopingInterval, intervalBasedLoopingEnabled: Boolean) -> Unit,
    onTapActionPickerClick: () -> Unit,
    onShapeClick: (String) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
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
                aspectRatio = photoWidget.aspectRatio,
                shapeId = photoWidget.shapeId,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = photoWidget.cornerRadius,
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
                    showMoveControls = photoWidget.photos.size > 1,
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
            photos = photoWidget.photos,
            onPhotoPickerClick = onPhotoPickerClick,
            onPhotoClick = onPhotoClick,
            aspectRatio = photoWidget.aspectRatio,
            shapeId = photoWidget.shapeId,
            modifier = Modifier.padding(top = 16.dp),
            cornerRadius = photoWidget.cornerRadius,
        )

        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TapActionPicker(
                tapAction = photoWidget.tapAction,
                onTapActionPickerClick = onTapActionPickerClick,
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

        AnimatedContent(
            targetState = photoWidget.aspectRatio,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "Customization_Picker",
        ) {
            if (it == PhotoWidgetAspectRatio.SQUARE) {
                ShapePicker(
                    shapeId = photoWidget.shapeId,
                    onShapeClick = onShapeClick,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                CornerRadiusPicker(
                    value = photoWidget.cornerRadius,
                    onValueChange = onCornerRadiusChange,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                    ),
                )
            }
        }

        FilledTonalButton(
            onClick = onAddToHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_configure_add_to_home))
        }

        Spacer(modifier = Modifier.size(24.dp))
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
    cornerRadius: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    PhotoWidgetShapeBuilder.buildShape(shapeId = shapeId)
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
                    cornerRadius = cornerRadius,
                    modifier = Modifier
                        .fillMaxHeight()
                        .conditional(
                            predicate = PhotoWidgetAspectRatio.ORIGINAL != aspectRatio,
                            ifTrue = { aspectRatio(ratio = aspectRatio.aspectRatio) },
                        )
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
                text = stringResource(id = tapAction.title),
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

@Composable
fun ShapedPhoto(
    photo: LocalPhoto,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    modifier: Modifier = Modifier,
    badge: @Composable BoxScope.() -> Unit = {},
) {
    val localInspectionMode = LocalInspectionMode.current
    val localContext = LocalContext.current

    val photoBitmap = remember(photo) {
        if (localInspectionMode) {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.widget_preview)
        } else {
            BitmapFactory.decodeFile(photo.path)
        }
    }

    val transformedBitmap = if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
        remember(photo, shapeId) {
            val shape = PhotoWidgetShapeBuilder.buildShape(
                shapeId = shapeId,
                width = photoBitmap.width.toFloat(),
                height = photoBitmap.height.toFloat(),
            )

            photoBitmap.withPolygonalShape(roundedPolygon = shape).asImageBitmap()
        }
    } else {
        remember(photo, aspectRatio, cornerRadius) {
            photoBitmap.withRoundedCorners(
                desiredAspectRatio = aspectRatio,
                radius = cornerRadius,
            ).asImageBitmap()
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
                .conditional(
                    predicate = PhotoWidgetAspectRatio.ORIGINAL != aspectRatio,
                    ifTrue = { aspectRatio(ratio = aspectRatio.aspectRatio) },
                )
                .fillMaxSize(),
            contentScale = if (PhotoWidgetAspectRatio.ORIGINAL != aspectRatio) {
                ContentScale.FillWidth
            } else {
                ContentScale.Fit
            },
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
private fun PhotoWidgetConfigureScreenPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                photos = listOf(
                    LocalPhoto(name = "photo-1", path = ""),
                    LocalPhoto(name = "photo-2", path = ""),
                ),
                loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
                tapAction = PhotoWidgetTapAction.VIEW_FULL_SCREEN,
                aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                shapeId = PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
                cornerRadius = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
            ),
            selectedPhoto = LocalPhoto(name = "photo-1", path = ""),
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onPhotoPickerClick = {},
            onPhotoClick = {},
            onLoopingIntervalPickerClick = { _, _ -> },
            onTapActionPickerClick = {},
            onShapeClick = {},
            onCornerRadiusChange = {},
            onAddToHomeClick = {},
            isProcessing = false,
        )
    }
}
