@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.viewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.chooser.PhotoWidgetChooserActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.model.rawAspectRatio
import com.fibelatti.photowidget.model.tapActionIncreaseBrightness
import com.fibelatti.photowidget.model.tapActionViewOriginalPhoto
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.platform.sharePhotoChooserIntent
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.ui.imageviewer.ZoomableImageViewer
import com.fibelatti.ui.imageviewer.rememberZoomableImageViewerState
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class PhotoWidgetViewerActivity : AppCompatActivity() {

    private val viewModel: PhotoWidgetViewerViewModel by viewModels()

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()

                state.photoWidget?.let { photoWidget ->
                    RememberedEffect(Unit) {
                        if (photoWidget.tapActionIncreaseBrightness) {
                            setScreenBrightness(value = 0.9f)
                        }
                    }

                    ScreenContent(
                        photo = photoWidget.currentPhoto,
                        isLoading = photoWidget.isLoading,
                        viewOriginalPhoto = photoWidget.tapActionViewOriginalPhoto,
                        aspectRatio = if (photoWidget.tapActionViewOriginalPhoto) {
                            PhotoWidgetAspectRatio.ORIGINAL
                        } else {
                            photoWidget.aspectRatio
                        },
                        onDismissClick = ::finishAndRemoveTask,
                        showFlipControls = state.showMoveControls,
                        onPreviousClick = viewModel::viewPreviousPhoto,
                        onNextClick = viewModel::viewNextPhoto,
                        onAllPhotosClick = ::showPhotoChooser,
                        onShareClick = ::sharePhoto,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onDestroy() {
        currentScreenBrightness?.let(::setScreenBrightness)
        super.onDestroy()
    }

    private fun setScreenBrightness(value: Float) {
        window?.attributes = window?.attributes?.apply {
            currentScreenBrightness = screenBrightness
            screenBrightness = value
        }
    }

    private fun showPhotoChooser() {
        val clickIntent = Intent(this, PhotoWidgetChooserActivity::class.java).apply {
            this.appWidgetId = intent.appWidgetId
        }
        startActivity(clickIntent)
    }

    private fun sharePhoto(photo: LocalPhoto) {
        val intent: Intent? = sharePhotoChooserIntent(
            context = this,
            originalPhotoPath = photo.originalPhotoPath,
            externalUri = photo.externalUri,
        )

        if (intent != null) {
            startActivity(intent)
        }
    }
}

private const val ANIM_DURATION: Int = 600

@Composable
private fun ScreenContent(
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
    var isBackgroundVisible: Boolean by remember { mutableStateOf(false) }
    var showControls: Boolean by remember { mutableStateOf(false) }

    val backgroundAlpha: Float by animateFloatAsState(
        targetValue = if (isBackgroundVisible) .8f else 0f,
        animationSpec = tween(ANIM_DURATION),
    )

    val imageViewerState = rememberZoomableImageViewerState(
        minimumScale = 1f,
        maximumScale = 4f,
        onTap = { showControls = !showControls },
        onDragToDismiss = onDismissClick,
    )

    LaunchedEffect(Unit) {
        isBackgroundVisible = true
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
        ZoomableImageViewer(state = imageViewerState) {
            var measuredRatio by remember { mutableFloatStateOf(aspectRatio.rawAspectRatio) }
            var didMeasure by remember { mutableStateOf(false) }

            AsyncPhotoViewer(
                data = photo?.getPhotoPath(viewOriginalPhoto = viewOriginalPhoto),
                dataKey = arrayOf(photo, aspectRatio),
                isLoading = isLoading,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.aspectRatio(ratio = measuredRatio),
                constraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.UNCONSTRAINED,
                transformer = { bitmap ->
                    measuredRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    didMeasure = true
                    bitmap
                },
            )

            LaunchedEffect(measuredRatio, didMeasure) {
                if (didMeasure) {
                    delay(100)
                    imageViewerState.animateToStandard()
                }
            }
        }

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
        ScreenContent(
            photo = LocalPhoto(photoId = "photo-1"),
            isLoading = false,
            viewOriginalPhoto = false,
            aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            onDismissClick = {},
            showFlipControls = true,
        )
    }
}
