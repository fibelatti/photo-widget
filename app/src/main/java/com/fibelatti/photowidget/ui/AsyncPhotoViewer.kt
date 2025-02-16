package com.fibelatti.photowidget.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.max
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.ui.foundation.dpToPx
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun AsyncPhotoViewer(
    data: Any?,
    dataKey: Array<Any?>,
    isLoading: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    constrainBitmapSize: Boolean = true,
    transformer: (Bitmap) -> Bitmap = { it },
    badge: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val localInspectionMode = LocalInspectionMode.current
        val localContext = LocalContext.current

        var photoBitmap: Bitmap? by remember {
            mutableStateOf(
                if (localInspectionMode) {
                    BitmapFactory.decodeResource(localContext.resources, R.drawable.widget_preview)
                } else {
                    null
                },
            )
        }
        val transformedBitmap: ImageBitmap? by remember {
            derivedStateOf {
                photoBitmap?.let(transformer)?.asImageBitmap()
            }
        }

        var showLoading: Boolean by remember { mutableStateOf(false) }
        var showError: Boolean by remember { mutableStateOf(false) }

        val decoder by remember {
            lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).photoDecoder() }
        }
        val largestSize = max(maxWidth, maxHeight)
        val maxDimension = if (constrainBitmapSize) {
            largestSize.dpToPx().roundToInt().coerceAtMost(maximumValue = PhotoWidget.MAX_WIDGET_DIMENSION)
        } else {
            null
        }

        LaunchedEffect(*dataKey) {
            if (localInspectionMode) return@LaunchedEffect
            if (data != null) {
                photoBitmap = decoder.decode(data = data, maxDimension = maxDimension)
                showLoading = false
                showError = false
            } else if (!isLoading) {
                showLoading = false
                showError = true
            }
        }

        LaunchedEffect(*dataKey) {
            // Avoid flickering the indicator, only show if the photos takes a while to load
            delay(timeMillis = 300)
            showLoading = isLoading || (data != null && photoBitmap == null && !showError)
        }

        val finalBitmap = transformedBitmap
        when {
            showLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(fraction = 0.8f),
                    )
                }
            }

            showError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_file_not_found),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(fraction = 0.8f),
                        contentScale = contentScale,
                    )
                }
            }

            finalBitmap != null -> {
                Image(
                    bitmap = finalBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )

                badge()
            }
        }
    }
}
