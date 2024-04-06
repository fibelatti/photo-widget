package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import javax.inject.Inject

class SavePhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
) {

    suspend operator fun invoke(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        photoWidgetStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)

        photoWidgetStorage.saveWidgetOrder(appWidgetId = appWidgetId, order = photoWidget.order)

        if (photoWidget.loopingEnabled) {
            photoWidgetAlarmManager.setup(
                appWidgetId = appWidgetId,
                repeatInterval = photoWidget.loopingInterval.repeatInterval,
                timeUnit = photoWidget.loopingInterval.timeUnit,
            )
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
        }

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
