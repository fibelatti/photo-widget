package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
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
            source = getWidgetSource(appWidgetId = appWidgetId) ?: PhotoWidgetSource.PHOTOS,
            syncedDir = getWidgetSyncDir(appWidgetId = appWidgetId),
            photos = getWidgetPhotos(appWidgetId = appWidgetId, index = currentIndex.takeIf { currentPhotoOnly }),
            currentIndex = currentIndex,
            shuffle = getWidgetShuffle(appWidgetId = appWidgetId),
            loopingInterval = getWidgetInterval(appWidgetId = appWidgetId) ?: PhotoWidgetLoopingInterval.ONE_DAY,
            intervalBasedLoopingEnabled = getWidgetIntervalEnabled(appWidgetId = appWidgetId),
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId) ?: PhotoWidgetTapAction.NONE,
            appShortcut = getWidgetAppShortcut(appWidgetId = appWidgetId),
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId) ?: PhotoWidgetAspectRatio.SQUARE,
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId) ?: PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
        )
    }
}
