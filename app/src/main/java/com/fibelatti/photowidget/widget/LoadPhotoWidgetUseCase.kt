package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class LoadPhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    operator fun invoke(appWidgetId: Int): Flow<PhotoWidget> = with(photoWidgetStorage) {
        Timber.d("Loading widget data (appWidgetId=$appWidgetId)")

        val widget = loadWidgetData(appWidgetId = appWidgetId)

        return flow {
            emit(widget.copy(isLoading = true))

            val currentPhotoId: String? = getCurrentPhotoId(appWidgetId = appWidgetId)
            val widgetPhotosFlow: Flow<PhotoWidget> = getWidgetPhotos(appWidgetId = appWidgetId)
                .map { widgetPhotos ->
                    val currentPhoto = widgetPhotos.current.run {
                        firstOrNull { it.photoId == currentPhotoId }
                            ?: getOrNull(getWidgetIndex(appWidgetId = appWidgetId))
                            ?: firstOrNull()
                    }

                    widget.copy(
                        photos = widgetPhotos.current,
                        currentPhoto = currentPhoto,
                        removedPhotos = widgetPhotos.excluded,
                        isLoading = false,
                    )
                }

            emitAll(widgetPhotosFlow)
        }
    }

    private fun loadWidgetData(appWidgetId: Int): PhotoWidget = with(photoWidgetStorage) {
        val (horizontalOffset: Int, verticalOffset: Int) = getWidgetOffset(appWidgetId = appWidgetId)
        val aspectRatio: PhotoWidgetAspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId)
        val cornerRadius: Int = if (PhotoWidgetAspectRatio.FILL_WIDGET == aspectRatio) {
            PhotoWidget.DEFAULT_CORNER_RADIUS
        } else {
            getWidgetCornerRadius(appWidgetId = appWidgetId)
        }
        val border: PhotoWidgetBorder = if (PhotoWidgetAspectRatio.FILL_WIDGET == aspectRatio) {
            PhotoWidgetBorder.None
        } else {
            getWidgetBorder(appWidgetId = appWidgetId)
        }

        return PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
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
            cornerRadius = cornerRadius,
            border = border,
            colors = PhotoWidgetColors(
                opacity = getWidgetOpacity(appWidgetId = appWidgetId),
                saturation = getWidgetSaturation(appWidgetId = appWidgetId),
                brightness = getWidgetBrightness(appWidgetId = appWidgetId),
            ),
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            padding = getWidgetPadding(appWidgetId = appWidgetId),
            deletionTimestamp = getWidgetDeletionTimestamp(appWidgetId = appWidgetId),
        )
    }
}
