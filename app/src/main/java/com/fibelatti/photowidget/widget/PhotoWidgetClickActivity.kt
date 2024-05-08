package com.fibelatti.photowidget.widget

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhotoWidgetClickActivity : AppCompatActivity() {

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        fadeIn()
        super.onCreate(savedInstanceState)

        setScreenBrightness(value = 0.9f)

        setContent {
            AppTheme {
                val viewModel: PhotoWidgetClickViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                state.photoWidget?.let { photoWidget ->
                    val currentPhoto = try {
                        photoWidget.currentPhoto
                    } catch (_: Exception) {
                        finish()
                        return@let
                    }

                    ScreenContent(
                        photo = currentPhoto,
                        aspectRatio = photoWidget.aspectRatio,
                        onDismiss = { finish() },
                        showFlipControls = state.showMoveControls,
                        onPreviousClick = { viewModel.flip(backwards = true) },
                        onNextClick = { viewModel.flip() },
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
    photo: LocalPhoto,
    aspectRatio: PhotoWidgetAspectRatio,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showFlipControls: Boolean = false,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
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

            if (showFlipControls) {
                FilledTonalIconButton(onClick = onNextClick) {
                    Icon(painterResource(id = R.drawable.ic_chevron_right), contentDescription = null)
                }
            }
        }

        AsyncPhotoViewer(
            data = when {
                !photo.path.isNullOrEmpty() -> photo.path
                photo.externalUri != null -> photo.externalUri
                else -> null
            },
            dataKey = arrayOf(photo, aspectRatio),
            contentScale = if (PhotoWidgetAspectRatio.ORIGINAL != aspectRatio) {
                ContentScale.FillWidth
            } else {
                ContentScale.Inside
            },
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                    onDoubleClick = {
                        scope.launch {
                            state.animateZoomBy(if (scale > 1f) 0.5f else 2f)
                            if (scale > 1f) offset = Offset.Zero
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
                .aspectRatio(ratio = aspectRatio.aspectRatio)
                .padding(all = 32.dp),
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun ScreenContentPreview() {
    ExtendedTheme {
        ScreenContent(
            photo = LocalPhoto(name = "photo-1"),
            aspectRatio = PhotoWidgetAspectRatio.SQUARE,
            onDismiss = {},
            showFlipControls = true,
        )
    }
}
