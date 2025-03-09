package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetColors
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
        val (horizontalOffset, verticalOffset) = getWidgetOffset(appWidgetId = appWidgetId)
        return PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            cycleMode = getWidgetCycleMode(appWidgetId = appWidgetId),
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId),
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId),
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId),
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
            border = getWidgetBorder(appWidgetId = appWidgetId),
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
