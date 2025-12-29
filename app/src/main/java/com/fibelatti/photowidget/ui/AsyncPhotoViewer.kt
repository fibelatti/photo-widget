package com.fibelatti.photowidget.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.getMaxBitmapWidgetDimension
import kotlin.math.max
import kotlinx.coroutines.delay

@Composable
fun AsyncPhotoViewer(
    data: Any?,
    dataKey: Array<Any?>,
    isLoading: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    constraintMode: AsyncPhotoViewer.BitmapSizeConstraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.DISPLAY,
    transformer: (Bitmap) -> Bitmap = { it },
) {
    var composableSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
    val largestSize: Int by remember {
        derivedStateOf { max(composableSize.width, composableSize.width) }
    }

    Box(
        modifier = modifier.onSizeChanged { newSize -> composableSize = newSize },
        contentAlignment = Alignment.Center,
    ) {
        val localInspectionMode: Boolean = LocalInspectionMode.current
        val localContext: Context = LocalContext.current
        val localResources: Resources = LocalResources.current

        var photoBitmap: Bitmap? by remember {
            mutableStateOf(
                if (localInspectionMode) {
                    BitmapFactory.decodeResource(localResources, R.drawable.widget_preview)
                } else {
                    null
                },
            )
        }
        val transformedBitmap: ImageBitmap? = remember(*dataKey.plus(photoBitmap)) {
            photoBitmap?.let(transformer)?.asImageBitmap()
        }

        var showLoading: Boolean by remember { mutableStateOf(false) }
        var showError: Boolean by remember { mutableStateOf(false) }

        val decoder: PhotoDecoder by remember {
            lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).photoDecoder() }
        }

        val maxWidgetDimension: Int = localContext.getMaxBitmapWidgetDimension(
            coerceMaxMemory = constraintMode == AsyncPhotoViewer.BitmapSizeConstraintMode.MEMORY,
        ).coerceAtMost(largestSize)

        LaunchedEffect(*dataKey.plus(largestSize)) {
            if (localInspectionMode) return@LaunchedEffect
            if (data != null) {
                photoBitmap = decoder.decode(
                    data = data,
                    maxDimension = if (constraintMode == AsyncPhotoViewer.BitmapSizeConstraintMode.UNCONSTRAINED) {
                        largestSize
                    } else {
                        maxWidgetDimension
                    },
                )
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

            transformedBitmap != null -> {
                Image(
                    bitmap = transformedBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }
        }
    }
}

object AsyncPhotoViewer {

    enum class BitmapSizeConstraintMode {

        /**
         * Constrain the size according to the maximum memory allowed for widget Bitmaps.
         */
        MEMORY,

        /**
         * Constrain the size according to the display size.
         */
        DISPLAY,

        /**
         * Do not apply any constraint and use the original bitmap size.
         */
        UNCONSTRAINED,
    }
}
