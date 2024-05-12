package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import timber.log.Timber
import javax.inject.Inject

class LoadPhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(
        appWidgetId: Int,
        currentPhotoOnly: Boolean = false,
    ): PhotoWidget = with(photoWidgetStorage) {
        Timber.d("Loading widget data (appWidgetId=$appWidgetId, currentPhotoOnly=$currentPhotoOnly)")

        val currentIndex = getWidgetIndex(appWidgetId = appWidgetId)

        return PhotoWidget(
            source = getWidgetSource(appWidgetId = appWidgetId),
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            photos = getWidgetPhotos(appWidgetId = appWidgetId, index = currentIndex.takeIf { currentPhotoOnly }),
            currentIndex = currentIndex,
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            loopingInterval = getWidgetInterval(appWidgetId = appWidgetId),
            intervalBasedLoopingEnabled = getWidgetIntervalEnabled(appWidgetId = appWidgetId),
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId),
            increaseBrightness = getWidgetIncreaseBrightness(appWidgetId = appWidgetId),
            appShortcut = getWidgetAppShortcut(appWidgetId = appWidgetId),
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId),
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId),
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
        )
    }
}
