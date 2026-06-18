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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSerializable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.canSort
import com.fibelatti.photowidget.platform.popNavKey
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.LocalSamplePhoto
import com.fibelatti.photowidget.ui.WidgetPositionViewer
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Back
import com.fibelatti.photowidget.ui.icons.ChevronLeft
import com.fibelatti.photowidget.ui.icons.ChevronRight
import com.fibelatti.photowidget.ui.icons.Crop
import com.fibelatti.photowidget.ui.icons.Trash
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

@Suppress("ktlint:compose:vm-forwarding-check")
@Composable
fun PhotoWidgetConfigureScreen(
    viewModel: PhotoWidgetConfigureViewModel,
    isUpdating: Boolean,
    onBack: () -> Unit,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()
    val isTransparent: Boolean = state.photoWidget.transparent

    val configureBackStack: NavBackStack<NavKey> = rememberSerializable(
        isTransparent,
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer()),
    ) {
        NavBackStack(
            if (isTransparent) PhotoWidgetConfigureNav.TapActionPicker else PhotoWidgetConfigureNav.Home,
        )
    }

    val localLifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val isLifecycleStateValid: () -> Boolean by rememberUpdatedState {
        localLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = viewModel::photoPicked,
    )

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::dirPicked,
    )

    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = viewModel::gifPicked,
    )

    SideEffect(state.messages) {
        val currentMessage: PhotoWidgetConfigureState.Message? = state.messages.firstOrNull()
        if (currentMessage is PhotoWidgetConfigureState.Message.LaunchCrop) {
            val key = PhotoWidgetConfigureNav.PhotoCrop(
                sourceUri = currentMessage.source.toString(),
                destinationUri = currentMessage.destination.toString(),
                aspectRatio = currentMessage.aspectRatio,
            )

            configureBackStack.add(key)
            viewModel.messageHandled(message = currentMessage)
        }
    }

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
                    onPickFromSourceClick = {
                        if (isLifecycleStateValid()) {
                            when (state.photoWidget.source) {
                                PhotoWidgetSource.PHOTOS -> photoPickerLauncher.launch(input = "image/*")
                                PhotoWidgetSource.DIRECTORY -> dirPickerLauncher.launch(input = null)
                                PhotoWidgetSource.GIF -> gifPickerLauncher.launch(input = "image/gif")
                            }
                        }
                    },
                )
            }

            entry<PhotoWidgetConfigureNav.TapActionPicker> {
                PhotoWidgetTapActionPicker(
                    onNavClick = { if (isTransparent) onBack() else configureBackStack.popNavKey() },
                    currentTapActions = state.photoWidget.tapActions,
                    source = state.photoWidget.source,
                    transparent = isTransparent,
                    onApplyClick = { actions ->
                        viewModel.tapActionSelected(actions)
                        if (isTransparent) {
                            viewModel.addNewWidget()
                        } else {
                            configureBackStack.popNavKey()
                        }
                    },
                )
            }

            entry<PhotoWidgetConfigureNav.PhotoCrop> { key: PhotoWidgetConfigureNav.PhotoCrop ->
                PhotoCropScreen(
                    sourceUri = key.sourceUri.toUri(),
                    destinationUri = key.destinationUri.toUri(),
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON_TOUCH,
                        showProgressBar = false,
                        maxZoom = 8,
                        fixAspectRatio = key.aspectRatio.isConstrained,
                        aspectRatioX = key.aspectRatio.x.roundToInt(),
                        aspectRatioY = key.aspectRatio.y.roundToInt(),
                    ),
                    onBackClick = {
                        viewModel.cropCancelled()
                        configureBackStack.popNavKey()
                    },
                    onCropComplete = { cropResult ->
                        val destinationPath: String? = key.destinationUri.toUri().path
                        if (cropResult.isSuccessful && cropResult.uriContent != null && destinationPath != null) {
                            viewModel.photoCropped(destinationPath)
                        } else {
                            viewModel.cropCancelled()
                        }
                        configureBackStack.popNavKey()
                    },
                    showAspectRatioShortcuts = !key.aspectRatio.isConstrained,
                )
            }
        },
    )
}

@Suppress("ktlint:compose:vm-forwarding-check")
@Composable
private fun PhotoWidgetConfigureHomeScreen(
    viewModel: PhotoWidgetConfigureViewModel,
    isUpdating: Boolean,
    onNav: (PhotoWidgetConfigureNav) -> Unit,
    onBack: () -> Unit,
    onPickFromSourceClick: () -> Unit,
) {
    val state: PhotoWidgetConfigureState by viewModel.state.collectAsStateWithLifecycle()

    val localBackHandler: OnBackPressedDispatcherOwner? = LocalOnBackPressedDispatcherOwner.current

    val tabContentScrollState: ScrollState = rememberScrollState()
    val tabContentModifier: Modifier = Modifier
        .fillMaxSize()
        .verticalScroll(tabContentScrollState)
        .padding(vertical = 16.dp)
        .fadingEdges(scrollState = tabContentScrollState)

    BackHandler(onBack = onBack)

    CompositionLocalProvider(LocalSamplePhoto provides state.selectedPhoto) {
        PhotoWidgetConfigureScreen(
            photoWidget = state.photoWidget,
            selectedPhoto = state.selectedPhoto,
            isProcessing = state.isProcessing,
            onNavClick = { localBackHandler?.onBackPressedDispatcher?.onBackPressed() },
            onCropClick = viewModel::requestCrop,
            onRemoveClick = viewModel::removePhoto,
            onMoveLeftClick = viewModel::moveLeft,
            onMoveRightClick = viewModel::moveRight,
            contentTab = {
                PhotoWidgetConfigureContentTab(
                    viewModel = viewModel,
                    onPickFromSourceClick = onPickFromSourceClick,
                )
            },
            appearanceTab = {
                PhotoWidgetConfigureAppearanceTab(
                    viewModel = viewModel,
                    modifier = tabContentModifier,
                )
            },
            textTab = {
                PhotoWidgetConfigureTextTab(
                    viewModel = viewModel,
                    modifier = tabContentModifier,
                )
            },
            behaviorTab = {
                PhotoWidgetConfigureBehaviorTab(
                    viewModel = viewModel,
                    onNav = onNav,
                    modifier = tabContentModifier,
                )
            },
            isUpdating = isUpdating,
            onAddToHomeClick = viewModel::addNewWidget,
        )
    }
}

@Composable
fun PhotoWidgetConfigureScreen(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    isProcessing: Boolean,
    onNavClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    contentTab: @Composable () -> Unit,
    appearanceTab: @Composable () -> Unit,
    textTab: @Composable () -> Unit,
    behaviorTab: @Composable () -> Unit,
    isUpdating: Boolean,
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
            selectedPhoto = selectedPhoto,
            isProcessing = isProcessing,
            onNavClick = onNavClick,
            onMoveLeftClick = onMoveLeftClick,
            onMoveRightClick = onMoveRightClick,
            onCropClick = onCropClick,
            onRemoveClick = onRemoveClick,
            contentTab = contentTab,
            appearanceTab = appearanceTab,
            textTab = textTab,
            behaviorTab = behaviorTab,
            isUpdating = isUpdating,
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
    selectedPhoto: LocalPhoto?,
    isProcessing: Boolean,
    onNavClick: () -> Unit,
    onCropClick: (LocalPhoto) -> Unit,
    onRemoveClick: (LocalPhoto) -> Unit,
    onMoveLeftClick: (LocalPhoto) -> Unit,
    onMoveRightClick: (LocalPhoto) -> Unit,
    contentTab: @Composable () -> Unit,
    appearanceTab: @Composable () -> Unit,
    textTab: @Composable () -> Unit,
    behaviorTab: @Composable () -> Unit,
    isUpdating: Boolean,
    onAddToHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPhotoWidget by rememberUpdatedState(photoWidget)
    val currentSelectedPhoto by rememberUpdatedState(selectedPhoto)
    val currentIsProcessing by rememberUpdatedState(isProcessing)
    val currentOnNavClick by rememberUpdatedState(onNavClick)
    val currentOnCropClick by rememberUpdatedState(onCropClick)
    val currentOnRemoveClick by rememberUpdatedState(onRemoveClick)
    val currentOnMoveLeftClick by rememberUpdatedState(onMoveLeftClick)
    val currentOnMoveRightClick by rememberUpdatedState(onMoveRightClick)
    val currentContentTab by rememberUpdatedState(contentTab)
    val currentAppearanceTab by rememberUpdatedState(appearanceTab)
    val currentTextTab by rememberUpdatedState(textTab)
    val currentBehaviorTab by rememberUpdatedState(behaviorTab)
    val currentIsUpdating by rememberUpdatedState(isUpdating)
    val currentOnAddToHomeClick by rememberUpdatedState(onAddToHomeClick)

    val viewer: @Composable (Modifier, WindowInsets) -> Unit = remember {
        movableContentOf { viewerModifier, insets ->
            PhotoWidgetViewer(
                photoWidget = currentPhotoWidget,
                selectedPhoto = currentSelectedPhoto,
                isProcessing = currentIsProcessing,
                onNavClick = currentOnNavClick,
                onCropClick = currentOnCropClick,
                onRemoveClick = currentOnRemoveClick,
                onMoveLeftClick = currentOnMoveLeftClick,
                onMoveRightClick = currentOnMoveRightClick,
                modifier = viewerModifier,
                editingControlsInsets = insets,
            )
        }
    }

    val editor: @Composable (WindowInsets) -> Unit = remember {
        movableContentOf { insets ->
            PhotoWidgetEditor(
                contentTab = currentContentTab,
                appearanceTab = currentAppearanceTab,
                textTab = currentTextTab,
                behaviorTab = currentBehaviorTab,
                isUpdating = currentIsUpdating,
                onAddToHomeClick = currentOnAddToHomeClick,
                contentWindowInsets = insets,
            )
        }
    }

    val isAtLeastMediumWidth: Boolean = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    if (!isAtLeastMediumWidth) {
        Column(modifier = modifier) {
            viewer(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                WindowInsets.safeDrawing
                    .only(sides = WindowInsetsSides.Start + WindowInsetsSides.Top)
                    .add(WindowInsets(bottom = 8.dp)),
            )

            editor(WindowInsets.navigationBars)
        }
    } else {
        Row(modifier = modifier) {
            viewer(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = 0.4f),
                WindowInsets.safeDrawing
                    .only(sides = WindowInsetsSides.Start + WindowInsetsSides.Vertical),
            )

            editor(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout.only(WindowInsetsSides.End)),
            )
        }
    }
}

// region Sections
@Composable
private fun PhotoWidgetViewer(
    photoWidget: PhotoWidget,
    selectedPhoto: LocalPhoto?,
    isProcessing: Boolean,
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

        var current: LocalPhoto? by remember(selectedPhoto) { mutableStateOf(selectedPhoto) }
        if (photoWidget.source == PhotoWidgetSource.GIF) {
            LaunchedEffect(photoWidget.photos, photoWidget.gifInterval, selectedPhoto, isProcessing) {
                if (photoWidget.photos.isEmpty() || isProcessing) return@LaunchedEffect

                for (photo in photoWidget.photos) {
                    delay(timeMillis = photoWidget.gifInterval)
                    ensureActive()
                    current = photo
                }
            }
        }

        if (selectedPhoto != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(editingControlsInsets),
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WidgetPositionViewer(
                    photoWidget = photoWidget.copy(currentPhoto = current),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(.75f)
                        .padding(horizontal = 8.dp),
                    areaColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                if (photoWidget.source != PhotoWidgetSource.GIF) {
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
                imageVector = AppIcons.Back,
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
                    imageVector = AppIcons.ChevronLeft,
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
                imageVector = AppIcons.Crop,
                contentDescription = stringResource(id = R.string.photo_widget_configure_menu_crop),
            )
        }

        FilledTonalIconButton(
            onClick = onRemoveClick,
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSources[2],
        ) {
            Icon(
                imageVector = AppIcons.Trash,
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
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = stringResource(id = R.string.photo_widget_configure_menu_move_right),
                )
            }
        }
    }
}

@Composable
private fun PhotoWidgetEditor(
    contentTab: @Composable () -> Unit,
    appearanceTab: @Composable () -> Unit,
    textTab: @Composable () -> Unit,
    behaviorTab: @Composable () -> Unit,
    isUpdating: Boolean,
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
        ) { tab: ConfigureTab ->
            when (tab) {
                ConfigureTab.CONTENT -> contentTab()
                ConfigureTab.APPEARANCE -> appearanceTab()
                ConfigureTab.TEXT -> textTab()
                ConfigureTab.BEHAVIOR -> behaviorTab()
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
@PreviewAll
private fun PhotoWidgetConfigureScreenPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
            ),
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            contentTab = {},
            appearanceTab = {},
            textTab = {},
            behaviorTab = {},
            isUpdating = false,
            onAddToHomeClick = {},
        )
    }
}

@Composable
@PreviewAll
private fun PhotoWidgetConfigureScreenTallPreview() {
    ExtendedTheme {
        PhotoWidgetConfigureScreen(
            photoWidget = PhotoWidget(
                source = PhotoWidgetSource.DIRECTORY,
                photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
                aspectRatio = PhotoWidgetAspectRatio.TALL,
                colors = PhotoWidgetColors(opacity = 80f),
            ),
            selectedPhoto = LocalPhoto(photoId = "photo-0"),
            isProcessing = false,
            onNavClick = {},
            onCropClick = {},
            onRemoveClick = {},
            onMoveLeftClick = {},
            onMoveRightClick = {},
            contentTab = {},
            appearanceTab = {},
            textTab = {},
            behaviorTab = {},
            isUpdating = false,
            onAddToHomeClick = {},
        )
    }
}
// endregion Previews
