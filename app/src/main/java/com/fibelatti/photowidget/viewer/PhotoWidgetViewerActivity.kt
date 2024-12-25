package com.fibelatti.photowidget.viewer

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateZoomBy
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhotoWidgetViewerActivity : AppCompatActivity() {

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        fadeIn()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val viewModel: PhotoWidgetViewerViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                state.photoWidget?.let { photoWidget ->
                    LaunchedEffect(Unit) {
                        if (photoWidget.increaseBrightness) {
                            setScreenBrightness(value = 0.9f)
                        }
                    }

                    ScreenContent(
                        photo = photoWidget.currentPhoto,
                        isLoading = photoWidget.isLoading,
                        viewOriginalPhoto = photoWidget.viewOriginalPhoto,
                        aspectRatio = if (photoWidget.viewOriginalPhoto) {
                            PhotoWidgetAspectRatio.ORIGINAL
                        } else {
                            photoWidget.aspectRatio
                        },
                        onDismiss = { finish() },
                        showFlipControls = state.showMoveControls,
                        onPreviousClick = { viewModel.flip(backwards = true) },
                        onNextClick = { viewModel.flip() },
                        showHint = state.showHint,
                    )
                }
            }
        }
    }

    private fun fadeIn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
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
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ScreenContent(
    photo: LocalPhoto?,
    isLoading: Boolean,
    viewOriginalPhoto: Boolean,
    aspectRatio: PhotoWidgetAspectRatio,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showFlipControls: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    showHint: Boolean = true,
) {
    var scale by remember { mutableFloatStateOf(1.1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncPhotoViewer(
            data = photo?.getPhotoPath(viewOriginalPhoto = viewOriginalPhoto),
            dataKey = arrayOf(photo, aspectRatio),
            isLoading = isLoading,
            contentScale = if (aspectRatio.isConstrained) {
                ContentScale.FillWidth
            } else {
                ContentScale.Fit
            },
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                    onDoubleClick = {
                        scope.launch {
                            state.animateZoomBy(if (scale > 2f) 0.5f else 2f)
                            if (scale > 2f) offset = Offset.Zero
                        }
                    },
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = state)
                .aspectRatio(ratio = aspectRatio.aspectRatio),
            constrainBitmapSize = false,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFlipControls) {
                FilledTonalIconButton(onClick = onPreviousClick) {
                    Icon(painterResource(id = R.drawable.ic_chevron_left), contentDescription = null)
                }
            }

            if (showHint) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.photo_widget_viewer_actions_pinch),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                    )
                    Text(
                        text = stringResource(id = R.string.photo_widget_viewer_actions_drag),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                    )
                    Text(
                        text = stringResource(id = R.string.photo_widget_viewer_actions_tap),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (showFlipControls) {
                FilledTonalIconButton(onClick = onNextClick) {
                    Icon(painterResource(id = R.drawable.ic_chevron_right), contentDescription = null)
                }
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
            onDismiss = {},
            showFlipControls = true,
        )
    }
}
