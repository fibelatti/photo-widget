package com.fibelatti.photowidget.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import kotlinx.coroutines.delay

@Composable
fun AsyncPhotoViewer(
    data: Any?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    transformer: (Bitmap?) -> Bitmap? = { it },
    vararg transformerKey: Any? = arrayOf(data, transformer),
    badge: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val localInspectionMode = LocalInspectionMode.current
        val localContext = LocalContext.current

        var photoBitmap: Bitmap? by remember(data) {
            mutableStateOf(
                if (localInspectionMode) {
                    BitmapFactory.decodeResource(localContext.resources, R.drawable.widget_preview)
                } else {
                    null
                },
            )
        }
        val transformedBitmap: ImageBitmap? by remember(transformerKey) {
            derivedStateOf {
                photoBitmap?.let { transformer(it) }?.asImageBitmap()
            }
        }

        var showLoading: Boolean by remember(data) { mutableStateOf(false) }

        val decoder by remember {
            lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).photoDecoder() }
        }
        val maxWidth = with(LocalDensity.current) {
            remember(maxWidth) { maxWidth.toPx().toInt() }
        }

        LaunchedEffect(key1 = data) {
            photoBitmap = decoder.decode(data = data, maxDimension = maxWidth)
            showLoading = false
        }

        LaunchedEffect(key1 = data) {
            // Avoid flickering the indicator, only show if the photos takes a while to load
            delay(timeMillis = 300)
            showLoading = photoBitmap == null
        }

        transformedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )

            badge()
        } ?: run {
            if (showLoading) {
                LoadingIndicator(
                    modifier = Modifier.padding(all = 4.dp),
                )
            }
        }
    }
}
