package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import com.fibelatti.photowidget.widget.PhotoWidgetWorkManager
import javax.inject.Inject

class SavePhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetWorkManager: PhotoWidgetWorkManager,
) {

    operator fun invoke(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        photoWidgetStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)

        photoWidgetStorage.saveWidgetOrder(appWidgetId = appWidgetId, order = photoWidget.order)

        if (photoWidget.loopingEnabled) {
            photoWidgetWorkManager.enqueueLoopingPhotoWidgetWork(
                appWidgetId = appWidgetId,
                repeatInterval = photoWidget.loopingInterval.repeatInterval,
                timeUnit = photoWidget.loopingInterval.timeUnit,
            )
        } else {
            photoWidgetWorkManager.cancelWidgetWork(appWidgetId = appWidgetId)
        }

        photoWidgetStorage.saveWidgetInterval(
            appWidgetId = appWidgetId,
            interval = photoWidget.loopingInterval,
        )

        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = if (photoWidget.loopingEnabled) {
                photoWidget.tapAction
            } else {
                PhotoWidgetTapAction.VIEW_FULL_SCREEN
            },
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
