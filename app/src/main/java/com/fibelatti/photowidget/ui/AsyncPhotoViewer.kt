package com.fibelatti.photowidget.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntSize
import coil3.transform.Transformation
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.getMaxBitmapWidgetDimension
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.FileNotFound
import kotlin.math.max
import kotlinx.coroutines.delay

@Composable
fun AsyncPhotoViewer(
    data: Any?,
    isLoading: Boolean,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    constraintMode: AsyncPhotoViewer.BitmapSizeConstraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.DISPLAY,
    transformations: List<Transformation> = emptyList(),
) {
    val localInspectionMode: Boolean = LocalInspectionMode.current
    val localContext: Context = LocalContext.current
    val localResources: Resources = LocalResources.current

    var viewerState: AsyncPhotoViewerState? by remember {
        mutableStateOf(
            if (localInspectionMode) {
                BitmapFactory.decodeResource(localResources, R.drawable.widget_preview)
                    ?.asImageBitmap()
                    ?.let(AsyncPhotoViewerState::Success)
            } else {
                null
            },
        )
    }

    var viewportSize: IntSize by remember { mutableStateOf(IntSize.Zero) }

    val decoder: PhotoDecoder by remember {
        lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).photoDecoder() }
    }

    val maxViewportDimension: Int = max(viewportSize.width, viewportSize.height)
    val maxWidgetDimension: Int = localContext.getMaxBitmapWidgetDimension(
        coerceMaxMemory = constraintMode == AsyncPhotoViewer.BitmapSizeConstraintMode.MEMORY,
    ).coerceAtMost(maxViewportDimension)
    val maxDimension: Int = when (constraintMode) {
        AsyncPhotoViewer.BitmapSizeConstraintMode.UNCONSTRAINED -> maxViewportDimension
        else -> maxWidgetDimension
    }
    // Snap to a coarser bucket so layout-pass jitter doesn't restart the decode effect.
    val maxDimensionBucketed: Int = remember(maxDimension) {
        if (maxDimension <= 0) maxDimension else ((maxDimension + 63) / 64) * 64
    }

    LaunchedEffect(data, maxDimensionBucketed, isLoading, transformations.joinToString { it.cacheKey }) {
        if (localInspectionMode || maxDimensionBucketed <= 0) return@LaunchedEffect

        when {
            data != null -> {
                decoder.decode(
                    data = data,
                    maxDimension = maxDimensionBucketed,
                    transformations = transformations,
                )?.asImageBitmap()?.let {
                    viewerState = AsyncPhotoViewerState.Success(it)
                }
            }

            !isLoading -> {
                viewerState = AsyncPhotoViewerState.Error
                return@LaunchedEffect
            }
        }

        // Avoid flickering the indicator, only show if the photo takes a while to load
        val isLoaded: () -> Boolean = {
            viewerState is AsyncPhotoViewerState.Success || viewerState is AsyncPhotoViewerState.Error
        }
        if (isLoading || !isLoaded()) {
            delay(timeMillis = 500)
            if (isLoading || !isLoaded()) {
                viewerState = AsyncPhotoViewerState.Loading
            }
        }
    }

    AsyncPhotoViewer(
        viewerState = viewerState,
        contentScale = contentScale,
        modifier = modifier.onSizeChanged { viewportSize = it },
    )
}

@Composable
private fun AsyncPhotoViewer(
    viewerState: AsyncPhotoViewerState?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = viewerState,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            fadeIn(animationSpec = tween(220, delayMillis = 90))
                .plus(scaleIn(initialScale = 0.96f, animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(ExitTransition.None)
        },
        contentAlignment = Alignment.Center,
        contentKey = { state -> state?.let { state::class } },
    ) { state: AsyncPhotoViewerState? ->
        when (state) {
            is AsyncPhotoViewerState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(fraction = 0.8f),
                    )
                }
            }

            is AsyncPhotoViewerState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        imageVector = AppIcons.FileNotFound,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(fraction = 0.8f),
                        contentScale = contentScale,
                    )
                }
            }

            is AsyncPhotoViewerState.Success -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }

            null -> Unit // Still determining the first state to include in the composition
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

private sealed class AsyncPhotoViewerState {

    data object Loading : AsyncPhotoViewerState()
    data object Error : AsyncPhotoViewerState()
    data class Success(val bitmap: ImageBitmap) : AsyncPhotoViewerState()
}
