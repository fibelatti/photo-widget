package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetLoadStage
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.resolveCurrentPhoto
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class LoadPhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    /**
     * Loads the widget data of [appWidgetId], emitting once per [PhotoWidgetLoadStage] reached, in
     * order. Collectors should stop once [PhotoWidget.loadStage] satisfies their use case, which
     * cancels the remaining stages:
     *
     * - Callers that only display the widget need [PhotoWidgetLoadStage.CURRENT_PHOTO], which
     * avoids potentially loading thousands of photos unnecessarily.
     * - Callers that need every photo but not the latest source state need
     * [PhotoWidgetLoadStage.CACHED_PHOTOS].
     *
     * When the local photo cache has no photos to show, the cached photos stage is skipped and the
     * source is read right away. So is the current photo stage, except for transparent widgets,
     * which always reach it without a photo.
     *
     * Stopping early relies on each stage only running once the previous emission is collected, so
     * the returned [Flow] **must not be buffered**, e.g. with `flowOn` or `buffer`.
     */
    operator fun invoke(appWidgetId: Int): Flow<PhotoWidget> = with(photoWidgetStorage) {
        Timber.i("Loading widget data %s", mapOf("appWidgetId" to appWidgetId))

        return flow {
            val widget = withContext(Dispatchers.IO) { loadWidgetData(appWidgetId = appWidgetId) }
            emit(widget.copy(loadStage = PhotoWidgetLoadStage.METADATA))

            if (widget.transparent) {
                emit(widget.copy(loadStage = PhotoWidgetLoadStage.CURRENT_PHOTO))
            } else {
                getCachedCurrentWidgetPhoto(appWidgetId = appWidgetId)?.let { currentPhoto: LocalPhoto ->
                    emit(widget.copy(currentPhoto = currentPhoto, loadStage = PhotoWidgetLoadStage.CURRENT_PHOTO))
                }
            }

            val currentPhotoId: String? = getCurrentPhotoId(appWidgetId = appWidgetId)
            val widgetIndex: Int = getWidgetIndex(appWidgetId = appWidgetId)

            loadWidgetPhotos(appWidgetId = appWidgetId).collect { widgetPhotos ->
                val currentPhoto: LocalPhoto? = widgetPhotos.current
                    .resolveCurrentPhoto(currentPhotoId = currentPhotoId, currentIndex = widgetIndex) { it.photoId }

                emit(
                    widget.copy(
                        photos = widgetPhotos.current,
                        currentPhoto = currentPhoto,
                        removedPhotos = widgetPhotos.excluded,
                        loadStage = if (widgetPhotos.fromCache) {
                            PhotoWidgetLoadStage.CACHED_PHOTOS
                        } else {
                            PhotoWidgetLoadStage.SOURCE_PHOTOS
                        },
                    ),
                )
            }
        }
    }

    private suspend fun loadWidgetData(appWidgetId: Int): PhotoWidget = with(photoWidgetStorage) {
        val aspectRatio: PhotoWidgetAspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId)
        val cornerRadius: Int
        val border: PhotoWidgetBorder
        val horizontalOffset: Int
        val verticalOffset: Int
        val padding: Int

        if (aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
            cornerRadius = PhotoWidget.DEFAULT_CORNER_RADIUS
            border = PhotoWidgetBorder.None
            horizontalOffset = 0
            verticalOffset = 0
            padding = 0
        } else {
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId)
            border = getWidgetBorder(appWidgetId = appWidgetId)

            val offset = getWidgetOffset(appWidgetId = appWidgetId)

            horizontalOffset = offset.horizontal
            verticalOffset = offset.vertical
            padding = getWidgetPadding(appWidgetId = appWidgetId)
        }

        return PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
            transparent = getWidgetTransparent(appWidgetId = appWidgetId),
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            directorySorting = getWidgetSorting(appWidgetId = appWidgetId),
            cycleMode = getWidgetCycleMode(appWidgetId = appWidgetId),
            tapActions = PhotoWidgetTapActions(
                left = getWidgetTapAction(appWidgetId = appWidgetId, tapActionArea = TapActionArea.LEFT),
                center = getWidgetTapAction(appWidgetId = appWidgetId, tapActionArea = TapActionArea.CENTER),
                right = getWidgetTapAction(appWidgetId = appWidgetId, tapActionArea = TapActionArea.RIGHT),
            ),
            aspectRatio = aspectRatio,
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId),
            shapeRotation = getWidgetShapeRotation(appWidgetId = appWidgetId),
            cornerRadius = cornerRadius,
            border = border,
            colors = PhotoWidgetColors(
                opacity = getWidgetOpacity(appWidgetId = appWidgetId),
                saturation = getWidgetSaturation(appWidgetId = appWidgetId),
                brightness = getWidgetBrightness(appWidgetId = appWidgetId),
            ),
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            padding = padding,
            text = getWidgetText(appWidgetId = appWidgetId),
            gifInterval = getWidgetGifInterval(appWidgetId = appWidgetId),
            deletionTimestamp = getWidgetDeletionTimestamp(appWidgetId = appWidgetId),
        )
    }
}
