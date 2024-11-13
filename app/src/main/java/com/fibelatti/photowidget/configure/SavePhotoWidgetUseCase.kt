package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
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

        photoWidgetStorage.saveWidgetCycleMode(
            appWidgetId = appWidgetId,
            cycleMode = photoWidget.cycleMode,
        )

        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = when {
                photoWidget.tapAction is PhotoWidgetTapAction.ViewInGallery &&
                    PhotoWidgetSource.PHOTOS == photoWidget.source -> PhotoWidgetTapAction.ViewFullScreen()

                else -> photoWidget.tapAction
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

        photoWidgetStorage.saveWidgetBorderColor(
            appWidgetId = appWidgetId,
            colorHex = photoWidget.borderColor.takeUnless {
                PhotoWidgetAspectRatio.FILL_WIDGET == photoWidget.aspectRatio
            },
            width = photoWidget.borderWidth,
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

        photoWidgetStorage.saveWidgetPadding(
            appWidgetId = appWidgetId,
            padding = photoWidget.padding,
        )

        if (photoWidget.cyclingEnabled) {
            photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
        }
    }
}
