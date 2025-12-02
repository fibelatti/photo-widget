@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.ui.foundation.DragState
import com.fibelatti.ui.foundation.onHorizontalDrag
import com.fibelatti.ui.foundation.onVerticalDrag
import com.fibelatti.ui.foundation.pxToDp
import com.fibelatti.ui.foundation.rememberDragState
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

private const val ANIM_DURATION: Int = 600

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
    aspectRatio: PhotoWidgetAspectRatio,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFlipControls: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onAllPhotosClick: () -> Unit = {},
    onShareClick: (LocalPhoto) -> Unit = {},
) {
    var showContent: Boolean by remember { mutableStateOf(false) }
    var showControls: Boolean by remember { mutableStateOf(false) }

    val backgroundAlpha: Float by animateFloatAsState(
        targetValue = if (showContent) .8f else 0f,
        animationSpec = tween(ANIM_DURATION),
    )
    val contentAlpha: Float by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(ANIM_DURATION),
    )

    val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
    val zoomState: ZoomState = rememberZoomState()
    val verticalDragState: DragState = rememberDragState(
        mode = DragState.Mode.UNIDIRECTIONAL,
        onConfirm = { onDismissClick() },
        onThreshold = { localHapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm) },
    )
    val horizontalDragState: DragState = rememberDragState(
        mode = DragState.Mode.BIDIRECTIONAL,
        onConfirm = { direction ->
            when (direction) {
                DragState.Direction.START -> onNextClick()
                DragState.Direction.END -> onPreviousClick()
            }
        },
        onThreshold = { localHapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm) },
    )

    val horizontalIndicatorSize: Dp by remember {
        derivedStateOf {
            (48 * horizontalDragState.currentOffsetFraction).dp
        }
    }

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        showContent = true
        showControls = true

        delay(ANIM_DURATION * 3L)
        showControls = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = horizontalDragState.currentOffsetPixel > MIN_OFFSET,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(all = 32.dp)
                .align(Alignment.CenterStart),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_left),
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
            visible = horizontalDragState.currentOffsetPixel < MIN_OFFSET * -1,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(all = 32.dp)
                .align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
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

        AsyncPhotoViewer(
            data = photo?.getPhotoPath(viewOriginalPhoto = viewOriginalPhoto),
            dataKey = arrayOf(photo, aspectRatio),
            isLoading = isLoading,
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val height = coordinates.size.height
                    val width = coordinates.size.width

                    if (height > width) {
                        verticalDragState.setThreshold(height * LARGEST_DIMENSION_FRACTION)
                        horizontalDragState.setThreshold(width * SMALLEST_DIMENSION_FRACTION)
                    } else {
                        verticalDragState.setThreshold(height * SMALLEST_DIMENSION_FRACTION)
                        horizontalDragState.setThreshold(width * LARGEST_DIMENSION_FRACTION)
                    }
                }
                .alpha(contentAlpha)
                .zoomable(
                    zoomState = zoomState,
                    onTap = { showControls = !showControls },
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
                    onDragStopped = {
                        coroutineScope.launch {
                            horizontalDragState.onDragStopped(resetOnConfirm = true)
                        }
                    },
                    enabled = showFlipControls && zoomState.scale == 1f,
                )
                .offset(
                    x = horizontalDragState.currentOffsetPixel.pxToDp(),
                    y = verticalDragState.currentOffsetPixel.pxToDp(),
                ),
            constraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.UNCONSTRAINED,
        )

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(all = 16.dp),
            enter = fadeIn(animationSpec = tween(ANIM_DURATION, delayMillis = 200)) + slideInVertically(
                animationSpec = tween(ANIM_DURATION),
                initialOffsetY = { -it },
            ),
            exit = fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutVertically(
                animationSpec = tween(ANIM_DURATION),
                targetOffsetY = { -it },
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showFlipControls) {
                    FilledTonalButton(
                        onClick = onAllPhotosClick,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.photo_widget_viewer_all_photos))

                        Spacer(modifier = Modifier.size(8.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_album),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (photo?.getPhotoPath(viewOriginalPhoto = true) != null) {
                    FilledTonalButton(
                        onClick = { onShareClick(photo) },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(text = stringResource(R.string.photo_widget_action_share))

                        Spacer(modifier = Modifier.size(8.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
            enter = fadeIn(animationSpec = tween(ANIM_DURATION, delayMillis = 200)) + slideInVertically(
                animationSpec = tween(ANIM_DURATION),
                initialOffsetY = { it },
            ),
            exit = fadeOut(animationSpec = tween(ANIM_DURATION)) + slideOutVertically(
                animationSpec = tween(ANIM_DURATION),
                targetOffsetY = { it },
            ),
        ) {
            Controls(
                showFlipControls = showFlipControls,
                onPreviousClick = onPreviousClick,
                onDismiss = onDismissClick,
                onNextClick = onNextClick,
            )
        }
    }
}

@Composable
private fun Controls(
    showFlipControls: Boolean,
    onPreviousClick: () -> Unit,
    onDismiss: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFlipControls) {
            FilledTonalIconButton(
                onClick = onPreviousClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            ) {
                Icon(painterResource(id = R.drawable.ic_chevron_left), contentDescription = null)
            }
        }

        Button(
            onClick = onDismiss,
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(text = stringResource(R.string.photo_widget_action_dismiss))
        }

        if (showFlipControls) {
            FilledTonalIconButton(
                onClick = onNextClick,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            ) {
                Icon(painterResource(id = R.drawable.ic_chevron_right), contentDescription = null)
            }
        }
    }
}

@Composable
@AllPreviews
private fun ScreenContentPreview() {
    ExtendedTheme {
        PhotoWidgetViewerScreen(
            photo = LocalPhoto(photoId = "photo-1"),
            isLoading = false,
            viewOriginalPhoto = false,
            aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            onDismissClick = {},
            showFlipControls = true,
        )
    }
}
