package com.fibelatti.photowidget.viewer

import android.app.KeyguardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.smallContainerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.photowidget.ui.icons.Album
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.ChevronLeft
import com.fibelatti.photowidget.ui.icons.ChevronRight
import com.fibelatti.photowidget.ui.icons.Share
import com.fibelatti.ui.foundation.DragState
import com.fibelatti.ui.foundation.onHorizontalDrag
import com.fibelatti.ui.foundation.onVerticalDrag
import com.fibelatti.ui.foundation.rememberDragState
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

private const val ANIM_DURATION: Int = 500

/**
 * The drag state uses a spring animation. Without a minimum threshold both indicators would
 * show when the subject is settling.
 */
private const val MIN_OFFSET: Int = 100

/**
 * Percent of the screen dimension required to trigger the drag state action.
 */
private const val SMALLEST_DIMENSION_FRACTION = .25f
private const val LARGEST_DIMENSION_FRACTION = .15f

@Composable
fun PhotoWidgetViewerScreen(
    photo: LocalPhoto?,
    isLoading: Boolean,
    viewOriginalPhoto: Boolean,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    showNextButton: Boolean = false,
    showPreviousButton: Boolean = false,
    showNavigationControls: Boolean = true,
    showPhotoPicker: Boolean = true,
    showShare: Boolean = true,
    showPhotoPath: Boolean = true,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onAllPhotosClick: () -> Unit = {},
    onShareClick: (LocalPhoto) -> Unit = {},
) {
    val localInspectionMode: Boolean = LocalInspectionMode.current
    var showControls: Boolean by remember { mutableStateOf(localInspectionMode) }

    // The picker and share sheet cannot be interacted with over the keyguard, so hide them while locked.
    val isDeviceLocked: Boolean = rememberIsDeviceLocked()

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    val localContext: Context = LocalContext.current
    val localClipboard: Clipboard = LocalClipboard.current

    LaunchedEffect(Unit) {
        showControls = true
        delay(timeMillis = ANIM_DURATION * 3L)
        showControls = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        FullScreenPhotoViewer(
            photo = photo,
            isLoading = isLoading,
            viewOriginalPhoto = viewOriginalPhoto,
            onPhotoClick = { showControls = !showControls },
            canDragToNext = showNextButton,
            canDragToPrevious = showPreviousButton,
            onDragToNext = onNextClick,
            onDragToPrevious = onPreviousClick,
            onDragToDismiss = onDismissClick,
        )

        ViewerHeaderControls(
            photo = photo,
            showControls = showControls,
            showPhotoPicker = showPhotoPicker && showNextButton && !isDeviceLocked,
            showShare = showShare && !isDeviceLocked,
            showPhotoPath = showPhotoPath,
            onAllPhotosClick = onAllPhotosClick,
            onShareClick = onShareClick,
            onPhotoPathClick = { path: String ->
                Toast.makeText(localContext, path, Toast.LENGTH_SHORT).show()
            },
            onPhotoPathLongClick = { path: String ->
                coroutineScope.launch {
                    localClipboard.setClipEntry(ClipData.newPlainText("", path).toClipEntry())
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(all = 16.dp),
        )

        ViewerFooterControls(
            showControls = showNavigationControls && showControls,
            showNextButton = showNextButton,
            showPreviousButton = showPreviousButton,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            onDismiss = onDismissClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun rememberIsDeviceLocked(): Boolean {
    if (LocalInspectionMode.current) return false

    val context: Context = LocalContext.current
    val keyguardManager: KeyguardManager? = remember(context) { context.getSystemService() }

    return remember(keyguardManager) { keyguardManager?.isKeyguardLocked == true }
}

@Composable
private fun FullScreenPhotoViewer(
    photo: LocalPhoto?,
    isLoading: Boolean,
    viewOriginalPhoto: Boolean,
    onPhotoClick: () -> Unit,
    canDragToNext: Boolean,
    canDragToPrevious: Boolean,
    onDragToNext: () -> Unit,
    onDragToPrevious: () -> Unit,
    onDragToDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current

    val zoomState: ZoomState = rememberZoomState()
    val verticalDragState: DragState = rememberDragState(
        mode = DragState.Mode.UNIDIRECTIONAL,
        onConfirm = { onDragToDismiss() },
        onThreshold = { localHapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
    )
    val horizontalDragState: DragState = rememberDragState(
        mode = DragState.Mode.BIDIRECTIONAL,
        onConfirm = { direction ->
            when (direction) {
                DragState.Direction.START if canDragToNext -> onDragToNext()
                DragState.Direction.END if canDragToPrevious -> onDragToPrevious()
                else -> Unit
            }
        },
        onThreshold = { localHapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
    )

    val horizontalIndicatorSize: Dp by remember {
        derivedStateOf {
            (48 * horizontalDragState.currentOffsetFraction).dp
        }
    }

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            visible = canDragToPrevious && horizontalDragState.currentOffsetPixel > MIN_OFFSET,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(all = 32.dp)
                .align(Alignment.CenterStart),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Icon(
                imageVector = AppIcons.ChevronLeft,
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .size(horizontalIndicatorSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        AnimatedVisibility(
            visible = canDragToNext && horizontalDragState.currentOffsetPixel < MIN_OFFSET * -1,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(all = 32.dp)
                .align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                    )
                    .size(horizontalIndicatorSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        if (photo != null) {
            AsyncPhotoViewer(
                data = photo.getPhotoPath(viewOriginalPhoto = viewOriginalPhoto),
                isLoading = isLoading,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .onSizeChanged { (width: Int, height: Int) ->
                        if (height > width) {
                            verticalDragState.setThreshold(height * LARGEST_DIMENSION_FRACTION)
                            horizontalDragState.setThreshold(width * SMALLEST_DIMENSION_FRACTION)
                        } else {
                            verticalDragState.setThreshold(height * SMALLEST_DIMENSION_FRACTION)
                            horizontalDragState.setThreshold(width * LARGEST_DIMENSION_FRACTION)
                        }
                    }
                    .zoomable(
                        zoomState = zoomState,
                        onTap = { onPhotoClick() },
                    )
                    .onVerticalDrag(
                        onDrag = verticalDragState::onDrag,
                        onDragStopped = {
                            coroutineScope.launch {
                                verticalDragState.onDragStopped()
                            }
                        },
                        enabled = zoomState.scale == 1f,
                    )
                    .onHorizontalDrag(
                        onDrag = { offset ->
                            horizontalDragState.onDrag(amount = offset)
                        },
                        onDragStop = {
                            coroutineScope.launch {
                                horizontalDragState.onDragStopped(resetOnConfirm = true)
                            }
                        },
                        enabled = canDragToNext && zoomState.scale == 1f,
                    )
                    .offset {
                        IntOffset(
                            x = horizontalDragState.currentOffsetPixel.toInt(),
                            y = verticalDragState.currentOffsetPixel.toInt(),
                        )
                    },
                constraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.UNCONSTRAINED,
            )
        }
    }
}

@Composable
private fun ViewerHeaderControls(
    photo: LocalPhoto?,
    showControls: Boolean,
    showPhotoPicker: Boolean,
    showShare: Boolean,
    showPhotoPath: Boolean,
    onAllPhotosClick: () -> Unit,
    onShareClick: (LocalPhoto) -> Unit,
    onPhotoPathClick: (String) -> Unit,
    onPhotoPathLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showControls,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(ANIM_DURATION, delayMillis = 200)) + slideInVertically(
            animationSpec = tween(ANIM_DURATION),
            initialOffsetY = { -it },
        ),
        exit = fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutVertically(
            animationSpec = tween(ANIM_DURATION),
            targetOffsetY = { -it },
        ),
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showPhotoPicker) {
                    FilledTonalButton(
                        onClick = onAllPhotosClick,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.photo_widget_viewer_all_photos))

                        Spacer(modifier = Modifier.size(8.dp))

                        Icon(
                            imageVector = AppIcons.Album,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (showShare && photo?.getPhotoPath(viewOriginalPhoto = true) != null) {
                    FilledTonalButton(
                        onClick = { onShareClick(photo) },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.photo_widget_action_share))

                        Spacer(modifier = Modifier.size(8.dp))

                        Icon(
                            imageVector = AppIcons.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            val path: String? = remember(photo?.externalUri) {
                photo?.externalPhotoPathString()
            }

            if (showPhotoPath && path != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction = .75f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = path,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.large,
                            )
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onPhotoPathClick(path) },
                                onLongClick = { onPhotoPathLongClick(path) },
                            )
                            .padding(all = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun LocalPhoto.externalPhotoPathString(): String? {
    externalUri ?: return null

    return externalUri.toString()
        .substringAfterLast("/")
        .replace("%3A", ":")
        .replace("%2F", "/")
        .replace("%20", " ")
        .substringAfter(":")
}

@Composable
private fun ViewerFooterControls(
    showControls: Boolean,
    showNextButton: Boolean,
    showPreviousButton: Boolean,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showControls,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(ANIM_DURATION, delayMillis = 200)) + slideInVertically(
            animationSpec = tween(ANIM_DURATION),
            initialOffsetY = { it },
        ),
        exit = fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutVertically(
            animationSpec = tween(ANIM_DURATION),
            targetOffsetY = { it },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onPreviousClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .visible(showPreviousButton)
                    .size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            ) {
                Icon(imageVector = AppIcons.ChevronLeft, contentDescription = null)
            }

            Button(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.photo_widget_action_dismiss))
            }

            FilledTonalIconButton(
                onClick = onNextClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .visible(showNextButton)
                    .size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            ) {
                Icon(imageVector = AppIcons.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
@PreviewAll
private fun ScreenContentPreview() {
    ExtendedTheme(darkTheme = true) {
        PhotoWidgetViewerScreen(
            photo = LocalPhoto(
                photoId = "photo-1",
                externalUri = "content://primary%3AImages%2FWidgets%2Fphoto-1".toUri(),
            ),
            isLoading = false,
            viewOriginalPhoto = false,
            onDismissClick = {},
            showNextButton = true,
            showPreviousButton = true,
        )
    }
}
