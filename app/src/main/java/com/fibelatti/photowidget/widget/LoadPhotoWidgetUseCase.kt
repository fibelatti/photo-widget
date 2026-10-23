package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
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
     * Loads the widget data of [appWidgetId]. Widget properties are emitted first, with its photos
     * following after. The returned [Flow] is expected to emit until [PhotoWidget.isLoading] is
     * false.
     *
     * When [currentPhotoOnly] is set, only [PhotoWidget.currentPhoto] is loaded from the local
     * photo cache, while [PhotoWidget.photos] and [PhotoWidget.removedPhotos] are left empty.
     * Callers that only display the widget should prefer it to avoid potentially loading thousands
     * of photos unnecessarily.
     *
     * Otherwise, photos are emitted first from the local photo cache and then from the widget
     * source. Callers that only need the cached photos can stop at the first emission where
     * [PhotoWidget.isLoading] is false, in which case the source is only read if the cache is empty.
     */
    operator fun invoke(
        appWidgetId: Int,
        currentPhotoOnly: Boolean = false,
    ): Flow<PhotoWidget> = with(photoWidgetStorage) {
        Timber.i(
            "Loading widget data %s",
            mapOf(
                "appWidgetId" to appWidgetId,
                "currentPhotoOnly" to currentPhotoOnly,
            ),
        )

        return flow {
            val widget = withContext(Dispatchers.IO) { loadWidgetData(appWidgetId = appWidgetId) }
            emit(widget.copy(isLoading = true))

            if (currentPhotoOnly) {
                val currentPhoto: LocalPhoto? = if (widget.transparent) {
                    null
                } else {
                    loadCurrentWidgetPhoto(appWidgetId = appWidgetId)
                }

                emit(widget.copy(currentPhoto = currentPhoto, isLoading = false))
                return@flow
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
                        isLoading = false,
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
