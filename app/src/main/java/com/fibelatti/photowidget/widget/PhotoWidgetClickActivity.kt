package com.fibelatti.photowidget.widget

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.fibelatti.photowidget.configure.ShapedPhoto
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoWidgetClickActivity : AppCompatActivity() {

    @Inject
    lateinit var loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appWidgetId = try {
            intent.appWidgetId
        } catch (_: Exception) {
            finishAffinity()
            return
        }

        lifecycleScope.launch {
            val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true)

            setContent {
                AppTheme {
                    ScreenContent(
                        photo = photoWidget.currentPhoto,
                        aspectRatio = photoWidget.aspectRatio,
                        shapeId = photoWidget.shapeId,
                        cornerRadius = photoWidget.cornerRadius,
                    )
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun ScreenContent(
        photo: LocalPhoto,
        aspectRatio: PhotoWidgetAspectRatio,
        shapeId: String,
        cornerRadius: Float,
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
                .background(color = Color.Black.copy(alpha = 0.6f))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { finishAffinity() },
                    onDoubleClick = {
                        scope.launch {
                            state.animateZoomBy(if (scale > 1f) 0.1f else 2f)
                            if (scale > 1f) offset = Offset.Zero
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ShapedPhoto(
                photo = photo,
                aspectRatio = aspectRatio,
                shapeId = shapeId,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = state)
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            )
        }
    }
}
