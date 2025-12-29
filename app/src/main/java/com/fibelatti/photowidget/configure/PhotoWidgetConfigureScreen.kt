@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.configure

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.canSort
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import com.fibelatti.photowidget.preferences.CornerRadiusPicker
import com.fibelatti.photowidget.preferences.OpacityPicker
import com.fibelatti.photowidget.preferences.ShapePicker
import com.fibelatti.photowidget.ui.BackgroundRestrictionBottomSheet
import com.fibelatti.photowidget.ui.BackgroundRestrictionWarningDialog
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.WidgetPositionViewer
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.foundation.hideBottomSheet
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetConfigureScreen(
    viewModel: PhotoWidgetConfigureViewModel,
    isUpdating: Boolean,
    onBack: () -> Unit,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()
    val configureBackStack: NavBackStack<NavKey> = rememberNavBackStack(PhotoWidgetConfigureNav.Home)

    NavDisplay(
        backStack = configureBackStack,
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
        },
        popTransitionSpec = {
            EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<PhotoWidgetConfigureNav.Home> {
                PhotoWidgetConfigureHomeScreen(
                    viewModel = viewModel,
                    isUpdating = isUpdating,
                    onNav = configureBackStack::add,
                    onBack = onBack,
                )
            }

            entry<PhotoWidgetConfigureNav.TapActionPicker> {
                PhotoWidgetTapActionPicker(
                    onNavClick = configureBackStack::pop,
                    currentTapActions = state.photoWidget.tapActions,
                    onApplyClick = { actions ->
                        viewModel.tapActionSelected(actions)
                        configureBackStack.pop()
                    },
                )
            }
        },
    )
}

private fun NavBackStack<*>.pop() {
    if (size > 1) removeLastOrNull()
}

@Composable
private fun PhotoWidgetConfigureHomeScreen(
    viewModel: PhotoWidgetConfigureViewModel,
    isUpdating: Boolean,
    onNav: (PhotoWidgetConfigureNav) -> Unit,
    onBack: () -> Unit,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    // region Sheet States
    var showBackgroundRestrictionDialog by remember { mutableStateOf(false) }
    val backgroundRestrictionSheetState = rememberAppSheetState()

    val aspectRatioPickerSheetState = rememberAppSheetState()
    val sourceSheetState = rememberAppSheetState()
    val importFromWidgetSheetState = rememberAppSheetState()
    val recentlyDeletedPhotoSheetState = rememberAppSheetState()
    val cycleModePickerSheetState = rememberAppSheetState()
    val directoryPickerSheetState = rememberAppSheetState()
    val shapePickerSheetState = rememberAppSheetState()
    val cornerRadiusPickerSheetState = rememberAppSheetState()
    val borderPickerSheetState = rememberAppSheetState()
    val opacityPickerSheetState = rememberAppSheetState()
    val saturationPickerSheetState = rememberAppSheetState()
    val brightnessPickerSheetState = rememberAppSheetState()
    val offsetPickerSheetState = rememberAppSheetState()
    val paddingPickerSheetState = rememberAppSheetState()
    // endregion Sheet States

    // region Picker Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = viewModel::photoPicked,
    )

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::dirPicked,
    )
    // endregion Picker Launchers

    val localBackHandler: OnBackPressedDispatcherOwner? = LocalOnBackPressedDispatcherOwner.current
    val localContext = LocalContext.current

    BackHandler(
        enabled = state.hasEdits,
        onBack = onBack,
    )

    CompositionLocalProvider(LocalSamplePhoto provides state.selectedPhoto) {
        PhotoWidgetConfigureScreen(
            photoWidget = state.photoWidget,
            isUpdating = isUpdating,
            selectedPhoto = state.selectedPhoto,
            isProcessing = state.isProcessing,
            onNavClick = { localBackHandler?.onBackPressedDispatcher?.onBackPressed() },
            onAspectRatioClick = aspectRatioPickerSheetState::showBottomSheet,
            onCropClick = viewModel::requestCrop,
            onRemoveClick = viewModel::removePhoto,
            onMoveLeftClick = viewModel::moveLeft,
            onMoveRightClick = viewModel::moveRight,
            onChangeSourceClick = sourceSheetState::showBottomSheet,
            isImportAvailable = state.isImportAvailable,
            onImportClick = importFromWidgetSheetState::showBottomSheet,
            onPhotoPickerClick = { photoPickerLauncher.launch(input = "image/*") },
            onDirPickerClick = { dirPickerLauncher.launch(input = null) },
            onPhotoClick = viewModel::previewPhoto,
            onReorderFinished = viewModel::reorderPhotos,
            onRemovedPhotoClick = { photo ->
                recentlyDeletedPhotoSheetState.showBottomSheet(data = photo)
            },
            onCycleModePickerClick = {
                if (localContext.isBackgroundRestricted(checkUnrestrictedBattery = true)) {
                    showBackgroundRestrictionDialog = true
                } else {
                    cycleModePickerSheetState.showBottomSheet()
                }
            },
            onShuffleChange = viewModel::saveShuffle,
            onSortClick = directoryPickerSheetState::showBottomSheet,
            onTapActionPickerClick = { onNav(PhotoWidgetConfigureNav.TapActionPicker) },
            onShapeClick = shapePickerSheetState::showBottomSheet,
            onCornerRadiusClick = cornerRadiusPickerSheetState::showBottomSheet,
            onBorderClick = borderPickerSheetState::showBottomSheet,
            onOpacityClick = opacityPickerSheetState::showBottomSheet,
            onSaturationClick = saturationPickerSheetState::showBottomSheet,
            onBrightnessClick = brightnessPickerSheetState::showBottomSheet,
            onOffsetClick = offsetPickerSheetState::showBottomSheet,
            onPaddingClick = paddingPickerSheetState::showBottomSheet,
            onPhotoWidgetTextChange = viewModel::photoWidgetTextChanged,
            onAddToHomeClick = viewModel::addNewWidget,
        )

        // region Bottom Sheets and Dialogs
        if (showBackgroundRestrictionDialog) {
            BackgroundRestrictionWarningDialog(
                onLearnMoreClick = {
                    showBackgroundRestrictionDialog = false
                    backgroundRestrictionSheetState.showBottomSheet()
                },
                onIgnoreClick = {
                    showBackgroundRestrictionDialog = false
                    cycleModePickerSheetState.showBottomSheet()
                },
            )
        }

        BackgroundRestrictionBottomSheet(
            sheetState = backgroundRestrictionSheetState,
        )

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

        ImportFromWidgetBottomSheet(
            sheetState = importFromWidgetSheetState,
            onWidgetSelected = viewModel::importFromWidget,
        )

        RecentlyDeletedPhotoBottomSheet(
            sheetState = recentlyDeletedPhotoSheetState,
            onRestore = viewModel::restorePhoto,
            onDelete = viewModel::deletePhotoPermanently,
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
            PhotoWidgetPaddingPicker(
                currentValue = state.photoWidget.padding,
                onApplyClick = { newValue ->
                    viewModel.paddingSelected(newValue)
                    paddingPickerSheetState.hideBottomSheet()
                },
            )
        }
        // endregion Bottom Sheets and Dialogs
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
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
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
    onPhotoWidgetTextChange: (PhotoWidgetText) -> Unit,
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
            isImportAvailable = isImportAvailable && !isProcessing,
            onImportClick = onImportClick,
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
            onPhotoWidgetTextChange = onPhotoWidgetTextChange,
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
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
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
    onPhotoWidgetTextChange: (PhotoWidgetText) -> Unit,
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
                        .height(360.dp),
                    editingControlsInsets = WindowInsets.safeDrawing
                        .only(sides = WindowInsetsSides.Start + WindowInsetsSides.Top),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSourceClick = onChangeSourceClick,
                    isImportAvailable = isImportAvailable,
                    onImportClick = onImportClick,
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
                    onPhotoWidgetTextChange = onPhotoWidgetTextChange,
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
                        .only(sides = WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                )

                PhotoWidgetEditor(
                    photoWidget = photoWidget,
                    isUpdating = isUpdating,
                    onChangeSourceClick = onChangeSourceClick,
                    isImportAvailable = isImportAvailable,
                    onImportClick = onImportClick,
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
                    onPhotoWidgetTextChange = onPhotoWidgetTextChange,
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
    editingControlsInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val gradientColors: List<Color> = listOf(
            Color.White,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        )
        val largeRadialGradient: Brush = object : ShaderBrush() {
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

        if (selectedPhoto != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(editingControlsInsets),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WidgetPositionViewer(
                    photoWidget = photoWidget.copy(currentPhoto = selectedPhoto),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(.75f)
                        .padding(horizontal = 8.dp),
                    areaColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                EditingControls(
                    onCropClick = { onCropClick(selectedPhoto) },
                    onRemoveClick = { onRemoveClick(selectedPhoto) },
                    showMoveControls = photoWidget.canSort,
                    moveLeftEnabled = photoWidget.photos.indexOf(selectedPhoto) != 0,
                    onMoveLeftClick = { onMoveLeftClick(selectedPhoto) },
                    moveRightEnabled = photoWidget.photos.indexOf(selectedPhoto) < photoWidget.photos.size - 1,
                    onMoveRightClick = { onMoveRightClick(selectedPhoto) },
                )
            }
        }

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

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMoveControls) {
            FilledTonalIconButton(
                onClick = onMoveLeftClick,
                shapes = IconButtonDefaults.shapes(),
                interactionSource = interactionSources[0],
                enabled = moveLeftEnabled,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_left),
                )
            }
        }

        FilledTonalIconButton(
            onClick = onCropClick,
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSources[1],
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_crop),
                contentDescription = stringResource(id = R.string.photo_widget_configure_menu_crop),
            )
        }

        FilledTonalIconButton(
            onClick = onRemoveClick,
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSources[2],
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = stringResource(id = R.string.photo_widget_configure_menu_remove),
            )
        }

        if (showMoveControls) {
            FilledTonalIconButton(
                onClick = onMoveRightClick,
                shapes = IconButtonDefaults.shapes(),
                enabled = moveRightEnabled,
                interactionSource = interactionSources[3],
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
private fun PhotoWidgetEditor(
    photoWidget: PhotoWidget,
    isUpdating: Boolean,
    onChangeSourceClick: () -> Unit,
    isImportAvailable: Boolean,
    onImportClick: () -> Unit,
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
    onPhotoWidgetTextChange: (PhotoWidgetText) -> Unit,
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
                    PhotoWidgetConfigureContentTab(
                        photoWidget = photoWidget,
                        onChangeSourceClick = onChangeSourceClick,
                        isImportAvailable = isImportAvailable,
                        onImportClick = onImportClick,
                        onPhotoPickerClick = onPhotoPickerClick,
                        onDirPickerClick = onDirPickerClick,
                        onPhotoClick = onPhotoClick,
                        onReorderFinished = onReorderFinished,
                        onRemovedPhotoClick = onRemovedPhotoClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ConfigureTab.APPEARANCE -> {
                    PhotoWidgetConfigureAppearanceTab(
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

                ConfigureTab.TEXT -> {
                    PhotoWidgetConfigureTextTab(
                        photoWidgetText = photoWidget.text,
                        onPhotoWidgetTextChange = onPhotoWidgetTextChange,
                        modifier = tabContentModifier,
                    )
                }

                ConfigureTab.BEHAVIOR -> {
                    PhotoWidgetConfigureBehaviorTab(
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
            isImportAvailable = true,
            onImportClick = {},
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
            onPhotoWidgetTextChange = {},
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
            isImportAvailable = true,
            onImportClick = {},
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
            onPhotoWidgetTextChange = {},
            onAddToHomeClick = {},
        )
    }
}
// endregion Previews
