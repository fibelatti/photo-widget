package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class LoadPhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    operator fun invoke(
        appWidgetId: Int,
        loadFromSource: Boolean,
    ): Flow<PhotoWidget> = with(photoWidgetStorage) {
        Timber.d("Loading widget data (appWidgetId=$appWidgetId, loadFromSource=$loadFromSource)")

        val (horizontalOffset, verticalOffset) = getWidgetOffset(appWidgetId = appWidgetId)
        val widget = PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            cycleMode = getWidgetCycleMode(appWidgetId = appWidgetId),
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId),
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId),
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId),
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
            borderColor = getWidgetBorderColorHex(appWidgetId = appWidgetId),
            borderWidth = getWidgetBorderWidth(appWidgetId = appWidgetId),
            opacity = getWidgetOpacity(appWidgetId = appWidgetId),
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            padding = getWidgetPadding(appWidgetId = appWidgetId),
            deletionTimestamp = getWidgetDeletionTimestamp(appWidgetId = appWidgetId),
        )

        return flow {
            emit(widget.copy(isLoading = true))

            val widgetPhotos = getWidgetPhotos(
                appWidgetId = appWidgetId,
                loadFromSource = loadFromSource,
            )
            val currentPhotoId = getCurrentPhotoId(appWidgetId = appWidgetId)
            val currentPhoto = widgetPhotos.current.run {
                firstOrNull { it.photoId == currentPhotoId }
                    ?: getOrNull(getWidgetIndex(appWidgetId = appWidgetId))
                    ?: firstOrNull()
            }

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
