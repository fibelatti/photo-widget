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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoWidgetClickActivity : AppCompatActivity() {

    @Inject
    lateinit var loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase

    private var currentScreenBrightness: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        fadeIn()
        super.onCreate(savedInstanceState)

        val appWidgetId = try {
            intent.appWidgetId
        } catch (_: Exception) {
            finish()
            return
        }

        setScreenBrightness(value = 0.9f)

        lifecycleScope.launch {
            val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true)

            setContent {
                AppTheme {
                    ScreenContent(
                        photo = photoWidget.currentPhoto,
                        aspectRatio = photoWidget.aspectRatio,
                        onDismiss = { finish() },
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
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.8f))
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
            ),
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(
            text = stringResource(id = R.string.photo_widget_viewer_actions),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 8.dp, end = 8.dp, bottom = 40.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray),
        )

        AsyncPhotoViewer(
            data = when {
                !photo.path.isNullOrEmpty() -> photo.path
                photo.externalUri != null -> photo.externalUri
                else -> null
            },
            contentScale = if (PhotoWidgetAspectRatio.ORIGINAL != aspectRatio) {
                ContentScale.FillWidth
            } else {
                ContentScale.Inside
            },
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = state)
                .fillMaxWidth()
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
        )
    }
}
