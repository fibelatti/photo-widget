package com.fibelatti.photowidget.configure

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.preferences.BooleanDefault
import com.fibelatti.photowidget.preferences.CornerRadiusPicker
import com.fibelatti.photowidget.preferences.DefaultPicker
import com.fibelatti.photowidget.preferences.OpacityPicker
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.preferences.ShapeDefault
import com.fibelatti.photowidget.preferences.ShapePicker
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun PhotoWidgetConfigureScreen(
    photoWidget: PhotoWidget,
    isUpdating: Boolean,
    selectedPhoto: LocalPhoto?,
    isProcessing: Boolean,
    onNavClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onCycleModePickerClick: (PhotoWidgetCycleMode) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction) -> Unit,
    onShapeChange: (String) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onBorderChange: (String?, Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlackAndWhiteChange: (Boolean) -> Unit,
    onOffsetChange: (horizontalOffset: Int, verticalOffset: Int) -> Unit,
    onPaddingChange: (Int) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val blurRadius by animateDpAsState(
            targetValue = if (isProcessing) 10.dp else 0.dp,
            label = "ProcessingBlur",
        )

        PhotoWidgetConfigureContent(
            photoWidget = photoWidget,
            isUpdating = isUpdating,
            selectedPhoto = selectedPhoto,
            onNavClick = onNavClick,
            onMoveLeftClick = onMoveLeftClick,
            onMoveRightClick = onMoveRightClick,
            onAspectRatioClick = onAspectRatioClick,
            onCropClick = onCropClick,
            onRemoveClick = onRemoveClick,
            onChangeSource = onChangeSource,
            onPhotoPickerClick = onPhotoPickerClick,
            onDirPickerClick = onDirPickerClick,
            onPhotoClick = onPhotoClick,
            onReorderFinished = onReorderFinished,
            onRemovedPhotoClick = onRemovedPhotoClick,
            onCycleModePickerClick = onCycleModePickerClick,
            onShuffleChange = onShuffleChange,
            onTapActionPickerClick = onTapActionPickerClick,
            onShapeClick = {
                ComposeBottomSheetDialog(localContext) {
                    ShapePicker(
                        onClick = { newShapeId ->
                            onShapeChange(newShapeId)
                            dismiss()
                        },
                        selectedShapeId = photoWidget.shapeId,
                    )
                }.show()
            },
            onCornerRadiusClick = {
                ComposeBottomSheetDialog(localContext) {
                    CornerRadiusPicker(
                        currentValue = photoWidget.cornerRadius,
                        onApplyClick = { newValue ->
                            onCornerRadiusChange(newValue)
                            dismiss()
                        },
                    )
                }.show()
            },
            onBorderClick = {
                PhotoWidgetBorderPicker.show(
                    context = localContext,
                    currentColorHex = photoWidget.borderColor,
                    currentWidth = photoWidget.borderWidth,
                    onApplyClick = onBorderChange,
                )
            },
            onOpacityClick = {
                ComposeBottomSheetDialog(localContext) {
                    OpacityPicker(
                        currentValue = photoWidget.opacity,
                        onApplyClick = { newValue ->
                            onOpacityChange(newValue)
                            dismiss()
                        },
                    )
                }.show()
            },
            onBlackAndWhiteChange = onBlackAndWhiteChange,
            onOffsetClick = {
                ComposeBottomSheetDialog(localContext) {
                    PhotoWidgetOffsetPicker(
                        horizontalOffset = photoWidget.horizontalOffset,
                        verticalOffset = photoWidget.verticalOffset,
                        onApplyClick = { newHorizontalOffset, newVerticalOffset ->
                            onOffsetChange(newHorizontalOffset, newVerticalOffset)
                            dismiss()
                        },
                    )
                }.show()
            },
            onPaddingClick = {
                ComposeBottomSheetDialog(localContext) {
                    PaddingPicker(
                        currentValue = photoWidget.padding,
                        onApplyClick = { newValue ->
                            onPaddingChange(newValue)
                            dismiss()
                        },
                    )
                }.show()
            },
            onAddToHomeClick = onAddToHomeClick,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = blurRadius),
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

@Composable
private fun PhotoWidgetConfigureContent(
    photoWidget: PhotoWidget,
    isUpdating: Boolean,
    selectedPhoto: LocalPhoto?,
    onNavClick: () -> Unit,
    onAspectRatioClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onCycleModePickerClick: (PhotoWidgetCycleMode) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction) -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onBlackAndWhiteChange: (Boolean) -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 840.dp) {
            Column {
                PhotoWidgetViewer(
                    photoWidget = photoWidget,
                    selectedPhoto = selectedPhoto,
                    onNavClick = onNavClick,
                    onCropClick = onCropClick,
                    onRemoveClick = onRemoveClick,
                    onMoveLeftClick = onMoveLeftClick,
                    onMoveRightClick = onMoveRightClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSource = onChangeSource,
                    onPhotoPickerClick = onPhotoPickerClick,
                    onDirPickerClick = onDirPickerClick,
                    onPhotoClick = onPhotoClick,
                    onReorderFinished = onReorderFinished,
                    onRemovedPhotoClick = onRemovedPhotoClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onShapeClick = onShapeClick,
                    onCornerRadiusClick = onCornerRadiusClick,
                    onBorderClick = onBorderClick,
                    onOpacityClick = onOpacityClick,
                    onBlackAndWhiteChange = onBlackAndWhiteChange,
                    onOffsetClick = onOffsetClick,
                    onPaddingClick = onPaddingClick,
                    onCycleModePickerClick = onCycleModePickerClick,
                    onShuffleChange = onShuffleChange,
                    onTapActionPickerClick = onTapActionPickerClick,
                    onAddToHomeClick = onAddToHomeClick,
                    contentWindowInsets = WindowInsets.navigationBars,
                )
            }
        } else {
            Row {
                PhotoWidgetViewer(
                    photoWidget = photoWidget,
                    selectedPhoto = selectedPhoto,
                    onNavClick = onNavClick,
                    onCropClick = onCropClick,
                    onRemoveClick = onRemoveClick,
                    onMoveLeftClick = onMoveLeftClick,
                    onMoveRightClick = onMoveRightClick,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = 0.4f),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSource = onChangeSource,
                    onPhotoPickerClick = onPhotoPickerClick,
                    onDirPickerClick = onDirPickerClick,
                    onPhotoClick = onPhotoClick,
                    onReorderFinished = onReorderFinished,
                    onRemovedPhotoClick = onRemovedPhotoClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onShapeClick = onShapeClick,
                    onCornerRadiusClick = onCornerRadiusClick,
                    onBorderClick = onBorderClick,
                    onOpacityClick = onOpacityClick,
                    onBlackAndWhiteChange = onBlackAndWhiteChange,
                    onOffsetClick = onOffsetClick,
                    onPaddingClick = onPaddingClick,
                    onCycleModePickerClick = onCycleModePickerClick,
                    onShuffleChange = onShuffleChange,
                    onTapActionPickerClick = onTapActionPickerClick,
                    onAddToHomeClick = onAddToHomeClick,
                    contentWindowInsets = WindowInsets.systemBars
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.End)),
                )
            }
        }
    }
}

// region Sections
@Composable
private fun PhotoWidgetViewer(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    onNavClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CurrentPhotoViewer(
            photo = selectedPhoto,
            aspectRatio = photoWidget.aspectRatio,
            shapeId = photoWidget.shapeId,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = photoWidget.cornerRadius,
            borderColorHex = photoWidget.borderColor,
            borderWidth = photoWidget.borderWidth,
            opacity = photoWidget.opacity,
            blackAndWhite = photoWidget.blackAndWhite,
        )

        IconButton(
            onClick = onNavClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding(),
            colors = IconButtonDefaults.iconButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = null,
            )
        }

        if (selectedPhoto != null) {
            EditingControls(
                onCropClick = { onCropClick(selectedPhoto) },
                onRemoveClick = { onRemoveClick(selectedPhoto) },
                showMoveControls = photoWidget.canSort,
                moveLeftEnabled = photoWidget.photos.indexOf(selectedPhoto) != 0,
                onMoveLeftClick = { onMoveLeftClick(selectedPhoto) },
                moveRightEnabled = photoWidget.photos.indexOf(selectedPhoto) < photoWidget.photos.size - 1,
                onMoveRightClick = { onMoveRightClick(selectedPhoto) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)),
            )
        }
    }
}

@Composable
private fun PhotoWidgetEditor(
    photoWidget: PhotoWidget,
    isUpdating: Boolean,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onAspectRatioClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onBlackAndWhiteChange: (Boolean) -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    onCycleModePickerClick: (PhotoWidgetCycleMode) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction) -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .windowInsetsPadding(contentWindowInsets),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConfigureTabs(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { tab ->
            val tabContentScrollState = rememberScrollState()
            val tabContentModifier = Modifier
                .fillMaxSize()
                .verticalScroll(tabContentScrollState)
                .padding(vertical = 16.dp)
                .fadingEdges(scrollState = tabContentScrollState)

            when (tab) {
                ConfigureTab.CONTENT -> {
                    ContentTab(
                        photoWidget = photoWidget,
                        onChangeSource = onChangeSource,
                        onPhotoPickerClick = onPhotoPickerClick,
                        onDirPickerClick = onDirPickerClick,
                        onPhotoClick = onPhotoClick,
                        onReorderFinished = onReorderFinished,
                        onRemovedPhotoClick = onRemovedPhotoClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ConfigureTab.APPEARANCE -> {
                    AppearanceTab(
                        photoWidget = photoWidget,
                        onAspectRatioClick = onAspectRatioClick,
                        onShapeClick = onShapeClick,
                        onCornerRadiusClick = onCornerRadiusClick,
                        onBorderClick = onBorderClick,
                        onOpacityClick = onOpacityClick,
                        onBlackAndWhiteChange = onBlackAndWhiteChange,
                        onOffsetClick = onOffsetClick,
                        onPaddingClick = onPaddingClick,
                        modifier = tabContentModifier,
                    )
                }

                ConfigureTab.BEHAVIOR -> {
                    BehaviorTab(
                        photoWidget = photoWidget,
                        onCycleModePickerClick = onCycleModePickerClick,
                        onShuffleChange = onShuffleChange,
                        onTapActionPickerClick = onTapActionPickerClick,
                        modifier = tabContentModifier,
                    )
                }
            }
        }

        FilledTonalButton(
            onClick = onAddToHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(
                    id = if (isUpdating) {
                        R.string.photo_widget_configure_save_changes
                    } else {
                        R.string.photo_widget_configure_add_to_home
                    },
                ),
            )
        }
    }
}
// endregion Sections

// region Tabs
@Composable
private fun ContentTab(
    photoWidget: PhotoWidget,
    onChangeSource: (currentSource: PhotoWidgetSource, syncedDir: Set<Uri>) -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    PhotoPicker(
        source = photoWidget.source,
        onChangeSource = { onChangeSource(photoWidget.source, photoWidget.syncedDir) },
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
        blackAndWhite = photoWidget.blackAndWhite,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun AppearanceTab(
    photoWidget: PhotoWidget,
    onAspectRatioClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onBlackAndWhiteChange: (Boolean) -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickerDefault(
            title = stringResource(id = R.string.photo_widget_aspect_ratio_title),
            currentValue = stringResource(id = photoWidget.aspectRatio.label),
            onClick = onAspectRatioClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (PhotoWidgetAspectRatio.SQUARE == photoWidget.aspectRatio) {
            ShapeDefault(
                title = stringResource(id = R.string.widget_defaults_shape),
                currentValue = photoWidget.shapeId,
                onClick = onShapeClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_corner_radius),
                currentValue = photoWidget.cornerRadius.toInt().toString(),
                onClick = onCornerRadiusClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(R.string.photo_widget_configure_border),
                currentValue = if (photoWidget.borderColor == null) {
                    stringResource(id = R.string.photo_widget_configure_border_none)
                } else {
                    "#${photoWidget.borderColor}"
                },
                onClick = onBorderClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_opacity),
            currentValue = photoWidget.opacity.toInt().toString(),
            onClick = onOpacityClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        BooleanDefault(
            title = stringResource(R.string.widget_defaults_black_and_white),
            currentValue = photoWidget.blackAndWhite,
            onCheckedChange = onBlackAndWhiteChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PickerDefault(
            title = stringResource(id = R.string.photo_widget_configure_offset),
            currentValue = stringResource(
                id = R.string.photo_widget_configure_offset_current_values,
                photoWidget.horizontalOffset,
                photoWidget.verticalOffset,
            ),
            onClick = onOffsetClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(id = R.string.photo_widget_configure_padding),
                currentValue = photoWidget.padding.toString(),
                onClick = onPaddingClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun BehaviorTab(
    photoWidget: PhotoWidget,
    onCycleModePickerClick: (PhotoWidgetCycleMode) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onTapActionPickerClick: (PhotoWidgetTapAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (photoWidget.photos.size > 1) {
            PickerDefault(
                title = stringResource(id = R.string.widget_defaults_cycling),
                currentValue = when (photoWidget.cycleMode) {
                    is PhotoWidgetCycleMode.Interval -> {
                        val intervalString = pluralStringResource(
                            id = when (photoWidget.cycleMode.loopingInterval.timeUnit) {
                                TimeUnit.SECONDS -> R.plurals.photo_widget_configure_interval_current_seconds
                                TimeUnit.MINUTES -> R.plurals.photo_widget_configure_interval_current_minutes
                                TimeUnit.HOURS -> R.plurals.photo_widget_configure_interval_current_hours
                                else -> R.plurals.photo_widget_configure_interval_current_days
                            },
                            count = photoWidget.cycleMode.loopingInterval.repeatInterval.toInt(),
                            photoWidget.cycleMode.loopingInterval.repeatInterval,
                        )
                        stringResource(id = R.string.photo_widget_configure_interval_current_label, intervalString)
                    }

                    is PhotoWidgetCycleMode.Schedule -> {
                        pluralStringResource(
                            id = R.plurals.photo_widget_configure_schedule_times,
                            count = photoWidget.cycleMode.triggers.size,
                            photoWidget.cycleMode.triggers.size,
                        )
                    }

                    is PhotoWidgetCycleMode.Disabled -> {
                        stringResource(id = R.string.photo_widget_configure_cycling_mode_disabled)
                    }
                },
                onClick = { onCycleModePickerClick(photoWidget.cycleMode) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (photoWidget.canShuffle) {
            BooleanDefault(
                title = stringResource(R.string.widget_defaults_shuffle),
                currentValue = photoWidget.shuffle,
                onCheckedChange = onShuffleChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_tap_action),
            currentValue = stringResource(id = photoWidget.tapAction.label),
            onClick = { onTapActionPickerClick(photoWidget.tapAction) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
// endregion Tabs

// region Components
@Composable
private fun CurrentPhotoViewer(
    photo: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    borderColorHex: String?,
    borderWidth: Int,
    opacity: Float,
    blackAndWhite: Boolean,
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
                opacity = opacity,
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start),
                    )
                    .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 48.dp)
                    .fillMaxHeight(),
                blackAndWhite = blackAndWhite,
                borderColorHex = borderColorHex,
                borderWidth = borderWidth,
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
// endregion Components

// region Pickers
@Composable
private fun PhotoPicker(
    source: PhotoWidgetSource,
    onChangeSource: () -> Unit,
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
    blackAndWhite: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val haptics = LocalHapticFeedback.current

        val currentPhotos by rememberUpdatedState(photos.toMutableStateList())
        val lazyGridState = rememberLazyGridState()
        val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
            currentPhotos.apply {
                add(index = to.index, element = removeAt(index = from.index))
            }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        opacity = PhotoWidget.DEFAULT_OPACITY,
                        modifier = Modifier
                            .animateItem()
                            .longPressDraggableHandle(
                                enabled = canSort,
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    onReorderFinished(currentPhotos)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                            .aspectRatio(ratio = 1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Image,
                                onClick = { onPhotoClick(photo) },
                            ),
                        blackAndWhite = blackAndWhite,
                    )
                }
            }
        }

        Row(
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
                .padding(all = 16.dp)
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    when (source) {
                        PhotoWidgetSource.PHOTOS -> onPhotoPickerClick()
                        PhotoWidgetSource.DIRECTORY -> onDirPickerClick()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
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
                onClick = onChangeSource,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.photo_widget_configure_change_source),
                    textAlign = TextAlign.Center,
                )
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
                    opacity = PhotoWidget.DEFAULT_OPACITY,
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
                    blackAndWhite = true,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PaddingPicker(
    currentValue: Int,
    onApplyClick: (newValue: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.photo_widget_configure_padding),
        modifier = modifier,
    ) {
        val localContext = LocalContext.current
        val baseBitmap = remember {
            BitmapFactory.decodeResource(localContext.resources, R.drawable.image_sample)
        }
        var value by remember(currentValue) { mutableIntStateOf(currentValue) }

        Image(
            bitmap = baseBitmap
                .withRoundedCorners(
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    radius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                )
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .padding(value.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { value = it.toInt() },
                modifier = Modifier.weight(1f),
                valueRange = 0f..20f,
                thumb = { SliderSmallThumb() },
            )

            Text(
                text = "$value",
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FilledTonalButton(
            onClick = { onApplyClick(value) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(text = stringResource(id = R.string.photo_widget_action_apply))
        }
    }
}
// endregion Pickers

// region Previews
@Composable
@AllPreviews
private fun PhotoWidgetConfigureScreenPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                source = PhotoWidgetSource.PHOTOS,
                photos = List(20) { index ->
                    LocalPhoto(photoId = "photo-$index")
                },
                shuffle = false,
                cycleMode = PhotoWidgetCycleMode.DEFAULT,
                tapAction = PhotoWidgetTapAction.DEFAULT,
                aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                shapeId = PhotoWidget.DEFAULT_SHAPE_ID,
                cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
            ),
            isUpdating = false,
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onChangeSource = { _, _ -> },
            onPhotoPickerClick = {},
            onDirPickerClick = {},
            onPhotoClick = {},
            onReorderFinished = {},
            onRemovedPhotoClick = {},
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onTapActionPickerClick = {},
            onShapeChange = {},
            onCornerRadiusChange = {},
            onBorderChange = { _, _ -> },
            onOpacityChange = {},
            onBlackAndWhiteChange = {},
            onOffsetChange = { _, _ -> },
            onPaddingChange = {},
            onAddToHomeClick = {},
        )
    }
}

@Composable
@AllPreviews
private fun PhotoWidgetConfigureScreenTallPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                source = PhotoWidgetSource.DIRECTORY,
                photos = List(20) { index ->
                    LocalPhoto(photoId = "photo-$index")
                },
                shuffle = false,
                cycleMode = PhotoWidgetCycleMode.DEFAULT,
                tapAction = PhotoWidgetTapAction.DEFAULT,
                aspectRatio = PhotoWidgetAspectRatio.TALL,
                shapeId = PhotoWidget.DEFAULT_SHAPE_ID,
                cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                opacity = 80f,
            ),
            isUpdating = true,
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onChangeSource = { _, _ -> },
            onPhotoPickerClick = {},
            onDirPickerClick = {},
            onPhotoClick = {},
            onReorderFinished = {},
            onRemovedPhotoClick = {},
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onTapActionPickerClick = {},
            onShapeChange = {},
            onCornerRadiusChange = {},
            onBorderChange = { _, _ -> },
            onOpacityChange = {},
            onBlackAndWhiteChange = {},
            onOffsetChange = { _, _ -> },
            onPaddingChange = {},
            onAddToHomeClick = {},
        )
    }
}
// endregion Previews
