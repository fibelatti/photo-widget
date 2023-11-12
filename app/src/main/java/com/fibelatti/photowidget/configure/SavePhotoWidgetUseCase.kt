package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import com.fibelatti.photowidget.widget.PhotoWidgetWorkManager
import javax.inject.Inject

class SavePhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetWorkManager: PhotoWidgetWorkManager,
) {

    operator fun invoke(
        appWidgetId: Int,
        enableLooping: Boolean,
        loopingInterval: PhotoWidgetLoopingInterval,
        aspectRatio: PhotoWidgetAspectRatio,
        shapeId: String,
    ) {
        photoWidgetStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)

        if (enableLooping) {
            photoWidgetWorkManager.enqueueLoopingPhotoWidgetWork(
                appWidgetId = appWidgetId,
                repeatInterval = loopingInterval.repeatInterval,
                timeUnit = loopingInterval.timeUnit,
            )
        } else {
            photoWidgetWorkManager.cancelWidgetWork(appWidgetId = appWidgetId)
        }

        photoWidgetStorage.saveWidgetInterval(
            appWidgetId = appWidgetId,
            interval = loopingInterval,
        )

        photoWidgetStorage.saveWidgetAspectRatio(
            appWidgetId = appWidgetId,
            aspectRatio = aspectRatio,
        )

        photoWidgetStorage.saveWidgetShapeId(
            appWidgetId = appWidgetId,
            shapeId = shapeId,
        )
    }
}
