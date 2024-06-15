package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import javax.inject.Inject
import timber.log.Timber

class SavePhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
) {

    suspend operator fun invoke(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        Timber.d("Saving widget data (appWidgetId=$appWidgetId)")

        photoWidgetStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)

        photoWidgetStorage.saveWidgetSource(appWidgetId = appWidgetId, source = photoWidget.source)

        when (photoWidget.source) {
            PhotoWidgetSource.PHOTOS -> {
                photoWidgetStorage.saveWidgetOrder(appWidgetId = appWidgetId, order = photoWidget.order)
            }

            PhotoWidgetSource.DIRECTORY -> {
                photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = photoWidget.syncedDir)
            }
        }

        photoWidgetStorage.saveWidgetShuffle(
            appWidgetId = appWidgetId,
            value = photoWidget.canShuffle && photoWidget.shuffle,
        )

        photoWidgetStorage.saveWidgetIntervalEnabled(
            appWidgetId = appWidgetId,
            value = photoWidget.loopingEnabled,
        )

        photoWidgetStorage.saveWidgetInterval(
            appWidgetId = appWidgetId,
            interval = photoWidget.loopingInterval,
        )

        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = when {
                PhotoWidgetTapAction.VIEW_IN_GALLERY == photoWidget.tapAction &&
                    PhotoWidgetSource.PHOTOS == photoWidget.source -> PhotoWidgetTapAction.VIEW_FULL_SCREEN

                else -> photoWidget.tapAction
            },
        )

        photoWidgetStorage.saveWidgetIncreaseBrightness(
            appWidgetId = appWidgetId,
            value = photoWidget.increaseBrightness,
        )

        photoWidgetStorage.saveWidgetAppShortcut(
            appWidgetId = appWidgetId,
            appName = photoWidget.appShortcut,
        )

        photoWidgetStorage.saveWidgetAspectRatio(
            appWidgetId = appWidgetId,
            aspectRatio = photoWidget.aspectRatio,
        )

        photoWidgetStorage.saveWidgetShapeId(
            appWidgetId = appWidgetId,
            shapeId = photoWidget.shapeId,
        )

        photoWidgetStorage.saveWidgetCornerRadius(
            appWidgetId = appWidgetId,
            cornerRadius = photoWidget.cornerRadius,
        )

        photoWidgetStorage.saveWidgetOpacity(
            appWidgetId = appWidgetId,
            opacity = photoWidget.opacity,
        )

        photoWidgetStorage.saveWidgetOffset(
            appWidgetId = appWidgetId,
            horizontalOffset = photoWidget.horizontalOffset,
            verticalOffset = photoWidget.verticalOffset,
        )

        if (photoWidget.loopingEnabled) {
            photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
        }
    }
}
