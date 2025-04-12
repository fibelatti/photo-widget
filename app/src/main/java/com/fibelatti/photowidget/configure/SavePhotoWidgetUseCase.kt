package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
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

        saveWidgetContent(appWidgetId = appWidgetId, photoWidget = photoWidget)
        saveWidgetAppearance(appWidgetId = appWidgetId, photoWidget = photoWidget)
        saveWidgetBehavior(appWidgetId = appWidgetId, photoWidget = photoWidget)

        if (photoWidget.cyclingEnabled) {
            photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
        }
    }

    private suspend fun saveWidgetContent(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        photoWidgetStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)

        photoWidgetStorage.saveWidgetSource(appWidgetId = appWidgetId, source = photoWidget.source)

        if (PhotoWidgetSource.DIRECTORY == photoWidget.source) {
            photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = photoWidget.syncedDir)
        }

        photoWidgetStorage.syncWidgetPhotos(
            appWidgetId = appWidgetId,
            currentPhotos = photoWidget.photos,
            removedPhotos = photoWidget.removedPhotos,
        )

        val currentPhotoId = photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
        val removedPhotos = photoWidget.removedPhotos.map { it.photoId }

        when {
            currentPhotoId == null -> {
                photoWidgetStorage.saveDisplayedPhoto(
                    appWidgetId = appWidgetId,
                    photoId = photoWidget.photos.first().photoId,
                )
            }

            currentPhotoId in removedPhotos && photoWidget.currentPhoto?.photoId != null -> {
                photoWidgetStorage.saveDisplayedPhoto(
                    appWidgetId = appWidgetId,
                    photoId = photoWidget.currentPhoto.photoId,
                )
            }
        }

        when (photoWidget.source) {
            PhotoWidgetSource.PHOTOS -> {
                photoWidgetStorage.markPhotosForDeletion(
                    appWidgetId = appWidgetId,
                    photoIds = removedPhotos,
                )
            }

            PhotoWidgetSource.DIRECTORY -> {
                photoWidgetStorage.saveExcludedPhotos(
                    appWidgetId = appWidgetId,
                    photoIds = removedPhotos,
                )
            }
        }
    }

    private fun saveWidgetAppearance(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
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
            cornerRadius = if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
                photoWidget.cornerRadius
            } else {
                PhotoWidget.DEFAULT_CORNER_RADIUS
            },
        )

        photoWidgetStorage.saveWidgetBorder(
            appWidgetId = appWidgetId,
            border = if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
                photoWidget.border
            } else {
                PhotoWidgetBorder.None
            },
        )

        photoWidgetStorage.saveWidgetOpacity(
            appWidgetId = appWidgetId,
            opacity = photoWidget.colors.opacity,
        )

        photoWidgetStorage.saveWidgetSaturation(
            appWidgetId = appWidgetId,
            saturation = photoWidget.colors.saturation,
        )

        photoWidgetStorage.saveWidgetBrightness(
            appWidgetId = appWidgetId,
            brightness = photoWidget.colors.brightness,
        )

        photoWidgetStorage.saveWidgetOffset(
            appWidgetId = appWidgetId,
            horizontalOffset = photoWidget.horizontalOffset,
            verticalOffset = photoWidget.verticalOffset,
        )

        photoWidgetStorage.saveWidgetPadding(
            appWidgetId = appWidgetId,
            padding = if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
                photoWidget.padding
            } else {
                0
            },
        )
    }

    private fun saveWidgetBehavior(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        photoWidgetStorage.saveWidgetShuffle(
            appWidgetId = appWidgetId,
            value = photoWidget.canShuffle && photoWidget.shuffle,
        )

        val currentCycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = appWidgetId)
        if (photoWidget.cycleMode != currentCycleMode) {
            photoWidgetStorage.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = null)
        }

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
    }
}
