package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import javax.inject.Inject

class LoadPhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    operator fun invoke(appWidgetId: Int): PhotoWidget = with(photoWidgetStorage) {
        return PhotoWidget(
            photos = getWidgetPhotos(appWidgetId = appWidgetId),
            currentIndex = getWidgetIndex(appWidgetId = appWidgetId),
            loopingInterval = getWidgetInterval(appWidgetId = appWidgetId) ?: PhotoWidgetLoopingInterval.ONE_DAY,
            tapAction = getWidgetTapAction(appWidgetId = appWidgetId) ?: PhotoWidgetTapAction.VIEW_FULL_SCREEN,
            aspectRatio = getWidgetAspectRatio(appWidgetId = appWidgetId) ?: PhotoWidgetAspectRatio.SQUARE,
            shapeId = getWidgetShapeId(appWidgetId = appWidgetId) ?: PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
            cornerRadius = getWidgetCornerRadius(appWidgetId = appWidgetId),
        )
    }
}
