@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.canShuffle
import com.fibelatti.photowidget.model.canSort
import com.fibelatti.photowidget.platform.formatPercent
import com.fibelatti.photowidget.platform.formatRangeValue
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.photowidget.preferences.BooleanDefault
import com.fibelatti.photowidget.preferences.CornerRadiusPicker
import com.fibelatti.photowidget.preferences.DefaultPicker
import com.fibelatti.photowidget.preferences.OpacityPicker
import com.fibelatti.photowidget.preferences.PickerDefault
import com.fibelatti.photowidget.preferences.ShapeDefault
import com.fibelatti.photowidget.preferences.ShapePicker
import com.fibelatti.photowidget.ui.AppBottomSheet
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.photowidget.ui.SliderSmallThumb
import com.fibelatti.photowidget.ui.hideBottomSheet
import com.fibelatti.photowidget.ui.rememberAppSheetState
import com.fibelatti.photowidget.ui.showBottomSheet
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import java.util.concurrent.TimeUnit
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun PhotoWidgetConfigureScreen(
    viewModel: PhotoWidgetConfigureViewModel,
    isUpdating: Boolean,
    onNavClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    val aspectRatioPickerSheetState = rememberAppSheetState()
    val sourceSheetState = rememberAppSheetState()
    val cycleModePickerSheetState = rememberAppSheetState()
    val directoryPickerSheetState = rememberAppSheetState()
    val tapActionPickerSheetState = rememberAppSheetState()
    val shapePickerSheetState = rememberAppSheetState()
    val cornerRadiusPickerSheetState = rememberAppSheetState()
    val borderPickerSheetState = rememberAppSheetState()
    val opacityPickerSheetState = rememberAppSheetState()
    val saturationPickerSheetState = rememberAppSheetState()
    val brightnessPickerSheetState = rememberAppSheetState()
    val offsetPickerSheetState = rememberAppSheetState()
    val paddingPickerSheetState = rememberAppSheetState()

    CompositionLocalProvider(LocalSamplePhoto provides state.selectedPhoto) {
        PhotoWidgetConfigureScreen(
            photoWidget = state.photoWidget,
            isUpdating = isUpdating,
            selectedPhoto = state.selectedPhoto,
            isProcessing = state.isProcessing,
            onNavClick = onNavClick,
            onAspectRatioClick = aspectRatioPickerSheetState::showBottomSheet,
            onCropClick = viewModel::requestCrop,
            onRemoveClick = viewModel::photoRemoved,
            onMoveLeftClick = viewModel::moveLeft,
            onMoveRightClick = viewModel::moveRight,
            onChangeSourceClick = sourceSheetState::showBottomSheet,
            onPhotoPickerClick = onPhotoPickerClick,
            onDirPickerClick = onDirPickerClick,
            onPhotoClick = viewModel::previewPhoto,
            onReorderFinished = viewModel::reorderPhotos,
            onRemovedPhotoClick = viewModel::restorePhoto,
            onCycleModePickerClick = cycleModePickerSheetState::showBottomSheet,
            onShuffleChange = viewModel::saveShuffle,
            onSortClick = directoryPickerSheetState::showBottomSheet,
            onTapActionPickerClick = tapActionPickerSheetState::showBottomSheet,
            onShapeClick = shapePickerSheetState::showBottomSheet,
            onCornerRadiusClick = cornerRadiusPickerSheetState::showBottomSheet,
            onBorderClick = borderPickerSheetState::showBottomSheet,
            onOpacityClick = opacityPickerSheetState::showBottomSheet,
            onSaturationClick = saturationPickerSheetState::showBottomSheet,
            onBrightnessClick = brightnessPickerSheetState::showBottomSheet,
            onOffsetClick = offsetPickerSheetState::showBottomSheet,
            onPaddingClick = paddingPickerSheetState::showBottomSheet,
            onAddToHomeClick = viewModel::addNewWidget,
        )

        // region Bottom Sheets
        PhotoWidgetAspectRatioBottomSheet(
            sheetState = aspectRatioPickerSheetState,
            onAspectRatioSelected = viewModel::setAspectRatio,
        )

        PhotoWidgetSourceBottomSheet(
            sheetState = sourceSheetState,
            currentSource = state.photoWidget.source,
            syncedDir = state.photoWidget.syncedDir,
            onDirRemoved = viewModel::removeDir,
            onChangeSource = viewModel::changeSource,
        )

        PhotoWidgetCycleModeBottomSheet(
            sheetState = cycleModePickerSheetState,
            cycleMode = state.photoWidget.cycleMode,
            onApplyClick = viewModel::cycleModeSelected,
        )

        DirectorySortingBottomSheet(
            sheetState = directoryPickerSheetState,
            onItemClick = viewModel::saveSorting,
        )

        PhotoWidgetTapActionBottomSheet(
            sheetState = tapActionPickerSheetState,
            currentTapActions = state.photoWidget.tapActions,
            onApplyClick = viewModel::tapActionSelected,
        )

        AppBottomSheet(
            sheetState = shapePickerSheetState,
        ) {
            ShapePicker(
                onClick = { newShapeId ->
                    viewModel.shapeSelected(newShapeId)
                    shapePickerSheetState.hideBottomSheet()
                },
                selectedShapeId = state.photoWidget.shapeId,
            )
        }

        AppBottomSheet(
            sheetState = cornerRadiusPickerSheetState,
        ) {
            CornerRadiusPicker(
                currentValue = state.photoWidget.cornerRadius,
                onApplyClick = { newValue ->
                    viewModel.cornerRadiusSelected(newValue)
                    cornerRadiusPickerSheetState.hideBottomSheet()
                },
            )
        }

        PhotoWidgetBorderBottomSheet(
            sheetState = borderPickerSheetState,
            currentBorder = state.photoWidget.border,
            onApplyClick = viewModel::borderSelected,
        )

        AppBottomSheet(
            sheetState = opacityPickerSheetState,
        ) {
            OpacityPicker(
                currentValue = state.photoWidget.colors.opacity,
                onApplyClick = { newValue ->
                    viewModel.opacitySelected(newValue)
                    opacityPickerSheetState.hideBottomSheet()
                },
            )
        }

        PhotoWidgetSaturationBottomSheet(
            sheetState = saturationPickerSheetState,
            currentSaturation = state.photoWidget.colors.saturation,
            onApplyClick = viewModel::saturationSelected,
        )

        PhotoWidgetBrightnessBottomSheet(
            sheetState = brightnessPickerSheetState,
            currentBrightness = state.photoWidget.colors.brightness,
            onApplyClick = viewModel::brightnessSelected,
        )

        AppBottomSheet(
            sheetState = offsetPickerSheetState,
        ) {
            PhotoWidgetOffsetPicker(
                horizontalOffset = state.photoWidget.horizontalOffset,
                verticalOffset = state.photoWidget.verticalOffset,
                onApplyClick = { newHorizontalOffset, newVerticalOffset ->
                    viewModel.offsetSelected(newHorizontalOffset, newVerticalOffset)
                    offsetPickerSheetState.hideBottomSheet()
                },
            )
        }

        AppBottomSheet(
            sheetState = paddingPickerSheetState,
        ) {
            PaddingPicker(
                currentValue = state.photoWidget.padding,
                onApplyClick = { newValue ->
                    viewModel.paddingSelected(newValue)
                    paddingPickerSheetState.hideBottomSheet()
                },
            )
        }
        // endregion Bottom Sheets
    }
}

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
    onChangeSourceClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onCycleModePickerClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
    onTapActionPickerClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onChangeSourceClick = onChangeSourceClick,
            onPhotoPickerClick = onPhotoPickerClick,
            onDirPickerClick = onDirPickerClick,
            onPhotoClick = onPhotoClick,
            onReorderFinished = onReorderFinished,
            onRemovedPhotoClick = onRemovedPhotoClick,
            onCycleModePickerClick = onCycleModePickerClick,
            onShuffleChange = onShuffleChange,
            onSortClick = onSortClick,
            onTapActionPickerClick = onTapActionPickerClick,
            onShapeClick = onShapeClick,
            onCornerRadiusClick = onCornerRadiusClick,
            onBorderClick = onBorderClick,
            onOpacityClick = onOpacityClick,
            onSaturationClick = onSaturationClick,
            onBrightnessClick = onBrightnessClick,
            onOffsetClick = onOffsetClick,
            onPaddingClick = onPaddingClick,
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
    onChangeSourceClick: () -> Unit,
    onPhotoPickerClick: () -> Unit,
    onDirPickerClick: () -> Unit,
    onPhotoClick: (LocalPhoto) -> Unit,
    onReorderFinished: (List<LocalPhoto>) -> Unit,
    onRemovedPhotoClick: (LocalPhoto) -> Unit,
    onCycleModePickerClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
    onTapActionPickerClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
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
                    editingControlsInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSourceClick = onChangeSourceClick,
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
                    onSaturationClick = onSaturationClick,
                    onBrightnessClick = onBrightnessClick,
                    onOffsetClick = onOffsetClick,
                    onPaddingClick = onPaddingClick,
                    onCycleModePickerClick = onCycleModePickerClick,
                    onShuffleChange = onShuffleChange,
                    onSortClick = onSortClick,
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
                    editingControlsInsets = WindowInsets.safeDrawing
                        .only(sides = WindowInsetsSides.Start + WindowInsetsSides.Bottom),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSourceClick = onChangeSourceClick,
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
                    onSaturationClick = onSaturationClick,
                    onBrightnessClick = onBrightnessClick,
                    onOffsetClick = onOffsetClick,
                    onPaddingClick = onPaddingClick,
                    onCycleModePickerClick = onCycleModePickerClick,
                    onShuffleChange = onShuffleChange,
                    onSortClick = onSortClick,
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
    editingControlsInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
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
            border = photoWidget.border,
            colors = photoWidget.colors,
        )

        IconButton(
            onClick = onNavClick,
            shapes = IconButtonDefaults.shapes(),
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
                    .windowInsetsPadding(editingControlsInsets),
            )
        }
    }
}

@Composable
private fun PhotoWidgetEditor(
    photoWidget: PhotoWidget,
    isUpdating: Boolean,
    onChangeSourceClick: () -> Unit,
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
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
    onOffsetClick: () -> Unit,
    onPaddingClick: () -> Unit,
    onCycleModePickerClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
    onTapActionPickerClick: () -> Unit,
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
                        onChangeSourceClick = onChangeSourceClick,
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
                        onSaturationClick = onSaturationClick,
                        onBrightnessClick = onBrightnessClick,
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
                        onSortClick = onSortClick,
                        onTapActionPickerClick = onTapActionPickerClick,
                        modifier = tabContentModifier,
                    )
                }
            }
        }

        Button(
            onClick = onAddToHomeClick,
            shapes = ButtonDefaults.shapes(),
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
    onChangeSourceClick: () -> Unit,
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
private fun AppearanceTab(
    photoWidget: PhotoWidget,
    onAspectRatioClick: () -> Unit,
    onShapeClick: () -> Unit,
    onCornerRadiusClick: () -> Unit,
    onBorderClick: () -> Unit,
    onOpacityClick: () -> Unit,
    onSaturationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
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
                currentValue = photoWidget.cornerRadius.toString(),
                onClick = onCornerRadiusClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            PickerDefault(
                title = stringResource(R.string.photo_widget_configure_border),
                currentValue = when (photoWidget.border) {
                    is PhotoWidgetBorder.None -> stringResource(id = R.string.photo_widget_configure_border_none)
                    is PhotoWidgetBorder.Color -> "#${photoWidget.border.colorHex}".toUpperCase(Locale.current)
                    is PhotoWidgetBorder.Dynamic -> stringResource(R.string.photo_widget_configure_border_dynamic)
                    is PhotoWidgetBorder.MatchPhoto -> {
                        when (photoWidget.border.type) {
                            PhotoWidgetBorder.MatchPhoto.Type.DOMINANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_dominant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.VIBRANT -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_vibrant)
                            }

                            PhotoWidgetBorder.MatchPhoto.Type.MUTED -> {
                                stringResource(R.string.photo_widget_configure_border_color_palette_muted)
                            }
                        }
                    }
                },
                onClick = onBorderClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_opacity),
            currentValue = formatPercent(value = photoWidget.colors.opacity, fractionDigits = 0),
            onClick = onOpacityClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PickerDefault(
            title = stringResource(R.string.widget_defaults_saturation),
            currentValue = formatRangeValue(value = PhotoWidgetColors.pickerSaturation(photoWidget.colors.saturation)),
            onClick = onSaturationClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        PickerDefault(
            title = stringResource(R.string.widget_defaults_brightness),
            currentValue = formatRangeValue(value = photoWidget.colors.brightness),
            onClick = onBrightnessClick,
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
    onCycleModePickerClick: () -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onSortClick: () -> Unit,
    onTapActionPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
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
                onClick = onCycleModePickerClick,
            )
        }

        if (photoWidget.canShuffle) {
            Spacer(modifier = Modifier.height(12.dp))

            BooleanDefault(
                title = stringResource(R.string.widget_defaults_shuffle),
                currentValue = photoWidget.shuffle,
                onCheckedChange = onShuffleChange,
            )
        }

        AnimatedVisibility(
            visible = PhotoWidgetSource.DIRECTORY == photoWidget.source && !photoWidget.shuffle,
        ) {
            PickerDefault(
                title = stringResource(R.string.photo_widget_directory_sort_title),
                currentValue = stringResource(id = photoWidget.directorySorting.label),
                onClick = onSortClick,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PickerDefault(
            title = stringResource(id = R.string.widget_defaults_tap_action),
            currentValue = buildString {
                appendLine(stringResource(id = photoWidget.tapActions.left.label))
                appendLine(stringResource(id = photoWidget.tapActions.center.label))
                appendLine(stringResource(id = photoWidget.tapActions.right.label))
            },
            onClick = onTapActionPickerClick,
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
    cornerRadius: Int,
    border: PhotoWidgetBorder,
    colors: PhotoWidgetColors,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val gradientColors = listOf(
            Color.White,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        )

        val largeRadialGradient = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = RadialGradientShader(
                colors = gradientColors,
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
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start),
                    )
                    .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 48.dp)
                    .fillMaxHeight(),
                colors = colors,
                border = border,
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
    val interactionSources: Array<MutableInteractionSource> = remember {
        Array(size = 4) { MutableInteractionSource() }
    }

    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMoveControls) {
            customItem(
                buttonGroupContent = {
                    FilledTonalIconButton(
                        onClick = onMoveLeftClick,
                        modifier = Modifier.animateWidth(interactionSources[0]),
                        interactionSource = interactionSources[0],
                        enabled = moveLeftEnabled,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_left),
                            contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_left),
                        )
                    }
                },
                menuContent = {},
            )
        }

        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = onCropClick,
                    modifier = Modifier.animateWidth(interactionSources[1]),
                    interactionSource = interactionSources[1],
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crop),
                        contentDescription = stringResource(id = R.string.photo_widget_configure_menu_crop),
                    )
                }
            },
            menuContent = {},
        )

        customItem(
            buttonGroupContent = {
                FilledTonalIconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.animateWidth(interactionSources[2]),
                    interactionSource = interactionSources[2],
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = stringResource(id = R.string.photo_widget_configure_menu_remove),
                    )
                }
            },
            menuContent = {},
        )

        if (showMoveControls) {
            customItem(
                buttonGroupContent = {
                    FilledTonalIconButton(
                        onClick = onMoveRightClick,
                        modifier = Modifier.animateWidth(interactionSources[3]),
                        enabled = moveRightEnabled,
                        interactionSource = interactionSources[3],
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_right),
                        )
                    }
                },
                menuContent = {},
            )
        }
    }
}
// endregion Components

// region Pickers
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PhotoPicker(
    source: PhotoWidgetSource,
    onChangeSourceClick: () -> Unit,
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
        val cacheWindow = LazyLayoutCacheWindow(aheadFraction = .5f, behindFraction = .5f)
        val lazyGridState = rememberLazyGridState(cacheWindow = cacheWindow)
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

        ButtonGroup(
            overflowIndicator = {},
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }

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
                            .heightIn(max = 36.dp)
                            .animateWidth(interactionSource),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        interactionSource = interactionSource,
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
                },
                menuContent = {},
            )

            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }

                    OutlinedButton(
                        onClick = onChangeSourceClick,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 36.dp)
                            .animateWidth(interactionSource),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        interactionSource = interactionSource,
                    ) {
                        AutoSizeText(
                            text = stringResource(R.string.photo_widget_configure_change_source),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                },
                menuContent = {},
            )
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
        var value by remember(currentValue) { mutableIntStateOf(currentValue) }

        Image(
            bitmap = rememberSampleBitmap()
                .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                .asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .padding((value * PhotoWidget.POSITIONING_MULTIPLIER).dp),
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

        Button(
            onClick = { onApplyClick(value) },
            shapes = ButtonDefaults.shapes(),
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
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
            ),
            isUpdating = false,
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onChangeSourceClick = {},
            onPhotoPickerClick = {},
            onDirPickerClick = {},
            onPhotoClick = {},
            onReorderFinished = {},
            onRemovedPhotoClick = {},
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onSortClick = {},
            onTapActionPickerClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onBorderClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onOffsetClick = {},
            onPaddingClick = {},
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
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
                aspectRatio = PhotoWidgetAspectRatio.TALL,
                colors = PhotoWidgetColors(opacity = 80f),
            ),
            isUpdating = false,
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onAspectRatioClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            onChangeSourceClick = {},
            onPhotoPickerClick = {},
            onDirPickerClick = {},
            onPhotoClick = {},
            onReorderFinished = {},
            onRemovedPhotoClick = {},
            onCycleModePickerClick = {},
            onShuffleChange = {},
            onSortClick = {},
            onTapActionPickerClick = {},
            onShapeClick = {},
            onCornerRadiusClick = {},
            onBorderClick = {},
            onOpacityClick = {},
            onSaturationClick = {},
            onBrightnessClick = {},
            onOffsetClick = {},
            onPaddingClick = {},
            onAddToHomeClick = {},
        )
    }
}
// endregion Previews
