package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import timber.log.Timber
import javax.inject.Inject

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
                photoWidget.syncedDir?.let {
                    photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = photoWidget.syncedDir)
                }
            }
        }

        if (photoWidget.loopingEnabled) {
            photoWidgetAlarmManager.setup(
                appWidgetId = appWidgetId,
                repeatInterval = photoWidget.loopingInterval.repeatInterval,
                timeUnit = photoWidget.loopingInterval.timeUnit,
            )
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
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
            tapAction = photoWidget.tapAction,
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
    }
}
