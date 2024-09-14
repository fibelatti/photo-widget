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
        originalPhotos: Boolean = false,
    ): Flow<PhotoWidget> = with(photoWidgetStorage) {
        Timber.d("Loading widget data (appWidgetId=$appWidgetId)")

        val currentIndex = getWidgetIndex(appWidgetId = appWidgetId)
        val (horizontalOffset, verticalOffset) = getWidgetOffset(appWidgetId = appWidgetId)
        val widget = PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            currentIndex = currentIndex,
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            cycleMode = getWidgetCycleMode(appWidgetId = appWidgetId),
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId),
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId),
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId),
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
            opacity = getWidgetOpacity(appWidgetId = appWidgetId),
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
            padding = getWidgetPadding(appWidgetId = appWidgetId),
            deletionTimestamp = getWidgetDeletionTimestamp(appWidgetId = appWidgetId),
        )

        return flow {
            emit(widget.copy(isLoading = true))
            emit(
                widget.copy(
                    photos = getWidgetPhotos(
                        appWidgetId = appWidgetId,
                        originalPhotos = widget.viewOriginalPhoto && originalPhotos,
                    ),
                    photosPendingDeletion = getPendingDeletionPhotos(appWidgetId = appWidgetId),
                    isLoading = false,
                ),
            )
        }
    }
}
