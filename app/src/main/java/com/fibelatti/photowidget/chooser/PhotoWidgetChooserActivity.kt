package com.fibelatti.photowidget.chooser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PhotoWidgetChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val viewModel: PhotoWidgetChooserViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()

                ScreenContent(
                    photos = state?.photos,
                    selectedPhoto = state?.currentPhoto,
                    onPhotoClick = { photo ->
                        scope.launch {
                            viewModel.setPhoto(photo = photo)

                            withContext(Dispatchers.Main) {
                                PhotoWidgetProvider.update(
                                    context = this@PhotoWidgetChooserActivity,
                                    appWidgetId = intent.appWidgetId,
                                )
                                finish()
                            }
                        }
                    },
                    onBackgroundClick = ::finish,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalStdlibApi::class, ExperimentalFoundationApi::class)
private fun ScreenContent(
    photos: List<LocalPhoto>?,
    selectedPhoto: LocalPhoto?,
    onPhotoClick: (LocalPhoto) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isBackgroundVisible: Boolean by remember { mutableStateOf(false) }
    val backgroundAlpha: Float by animateFloatAsState(
        targetValue = if (isBackgroundVisible) .8f else 0f,
        animationSpec = tween(600),
    )

    RememberedEffect(Unit) {
        isBackgroundVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = backgroundAlpha))
            .clickable(onClick = onBackgroundClick)
            .safeDrawingPadding()
            .padding(horizontal = 48.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (photos == null) {
            LoadingIndicator(modifier = Modifier.size(48.dp))
            return
        }

        val cacheWindow = LazyLayoutCacheWindow(aheadFraction = .5f, behindFraction = .5f)
        val lazyGridState = rememberLazyGridState(cacheWindow = cacheWindow)
        val currentPhotos by rememberUpdatedState(photos.toMutableStateList())

        LazyVerticalGrid(
            columns = GridCells.Fixed(count = 5),
            modifier = Modifier
                .background(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface)
                .padding(all = 16.dp)
                .fadingEdges(scrollState = lazyGridState),
            state = lazyGridState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(
                key = "title",
                span = { GridItemSpan(currentLineSpan = 5) },
            ) {
                Text(
                    text = stringResource(R.string.photo_widget_configure_tap_action_choose_next_photo),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            items(currentPhotos, key = { photo -> photo }) { photo ->
                ShapedPhoto(
                    photo = photo,
                    aspectRatio = PhotoWidgetAspectRatio.SQUARE,
                    shapeId = PhotoWidget.DEFAULT_SHAPE_ID,
                    cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS,
                    modifier = Modifier
                        .aspectRatio(ratio = 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Image,
                            onClick = { onPhotoClick(photo) },
                        ),
                    border = if (photo.photoId == selectedPhoto?.photoId) {
                        PhotoWidgetBorder.Color(
                            colorHex = MaterialTheme.colorScheme.primary.toArgb().toHexString(),
                            width = 100,
                        )
                    } else {
                        PhotoWidgetBorder.None
                    },
                )
            }
        }
    }
}

@Composable
@AllPreviews
private fun ScreenContentPreview() {
    ExtendedTheme {
        ScreenContent(
            photos = List(20) { index -> LocalPhoto(photoId = "photo-$index") },
            selectedPhoto = LocalPhoto(photoId = "photo-3"),
            onPhotoClick = {},
            onBackgroundClick = {},
        )
    }
}
