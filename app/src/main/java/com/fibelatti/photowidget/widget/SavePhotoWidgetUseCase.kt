package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.canShuffle
import com.fibelatti.photowidget.model.coerceTapActions
import com.fibelatti.photowidget.model.photoCycleEnabled
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import timber.log.Timber

class SavePhotoWidgetUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
) {

    suspend operator fun invoke(
        draftWidgetId: Int,
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        Timber.i("Saving widget data (draftWidgetId=$draftWidgetId, appWidgetId=$appWidgetId)")

        saveWidgetContent(draftWidgetId = draftWidgetId, appWidgetId = appWidgetId, photoWidget = photoWidget)
        saveWidgetAppearance(appWidgetId = appWidgetId, photoWidget = photoWidget)
        saveWidgetBehavior(appWidgetId = appWidgetId, photoWidget = photoWidget)

        photoWidgetStorage.saveWidgetText(appWidgetId = appWidgetId, text = photoWidget.text)

        // Draft widgets won't have alarms yet...
        if (PhotoWidget.isDraftWidgetId(appWidgetId)) return

        if (photoWidget.photoCycleEnabled) {
            photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
        }
    }

    private suspend fun saveWidgetContent(
        draftWidgetId: Int,
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        if (draftWidgetId != appWidgetId) {
            photoWidgetStorage.migrateDraftToWidget(draftWidgetId = draftWidgetId, appWidgetId = appWidgetId)
        }

        photoWidgetStorage.saveWidgetSource(appWidgetId = appWidgetId, source = photoWidget.source)

        if (PhotoWidgetSource.DIRECTORY == photoWidget.source) {
            photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = photoWidget.syncedDir)
        }

        photoWidgetStorage.syncWidgetPhotos(
            appWidgetId = appWidgetId,
            currentPhotos = photoWidget.photos,
            removedPhotos = photoWidget.removedPhotos,
        )

        val isDraftWidget: Boolean = PhotoWidget.isDraftWidgetId(appWidgetId)
        val currentPhotoId: String? = photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
        val removedPhotoIds: List<String> = photoWidget.removedPhotos.map { it.photoId }

        when (currentPhotoId) {
            null if !isDraftWidget -> {
                photoWidgetStorage.saveDisplayedPhoto(
                    appWidgetId = appWidgetId,
                    photoId = photoWidget.photos.first().photoId,
                )
            }

            in removedPhotoIds if photoWidget.currentPhoto?.photoId != null && !isDraftWidget -> {
                photoWidgetStorage.saveDisplayedPhoto(
                    appWidgetId = appWidgetId,
                    photoId = photoWidget.currentPhoto.photoId,
                )
            }
        }

        when {
            isDraftWidget -> {
                photoWidgetStorage.deletePhotos(
                    appWidgetId = appWidgetId,
                    photoIds = removedPhotoIds,
                )
            }

            PhotoWidgetSource.PHOTOS == photoWidget.source -> {
                photoWidgetStorage.replacePhotosForDeletion(
                    appWidgetId = appWidgetId,
                    photoIds = removedPhotoIds,
                )
            }

            PhotoWidgetSource.DIRECTORY == photoWidget.source -> {
                photoWidgetStorage.replaceExcludedPhotos(
                    appWidgetId = appWidgetId,
                    photoIds = removedPhotoIds,
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

        if (PhotoWidgetAspectRatio.FILL_WIDGET != photoWidget.aspectRatio) {
            photoWidgetStorage.saveWidgetOffset(
                appWidgetId = appWidgetId,
                horizontalOffset = photoWidget.horizontalOffset,
                verticalOffset = photoWidget.verticalOffset,
            )

            photoWidgetStorage.saveWidgetPadding(
                appWidgetId = appWidgetId,
                padding = photoWidget.padding,
            )
        } else {
            photoWidgetStorage.saveWidgetOffset(appWidgetId = appWidgetId, horizontalOffset = 0, verticalOffset = 0)
            photoWidgetStorage.saveWidgetPadding(appWidgetId = appWidgetId, padding = 0)
        }
    }

    private fun saveWidgetBehavior(
        appWidgetId: Int,
        photoWidget: PhotoWidget,
    ) {
        photoWidgetStorage.saveWidgetShuffle(
            appWidgetId = appWidgetId,
            value = photoWidget.canShuffle && photoWidget.shuffle,
        )

        photoWidgetStorage.saveWidgetSorting(
            appWidgetId = appWidgetId,
            sorting = photoWidget.directorySorting,
        )

        val currentCycleMode: PhotoWidgetCycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = appWidgetId)
        if (photoWidget.cycleMode != currentCycleMode) {
            photoWidgetStorage.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = null)
        }

        photoWidgetStorage.saveWidgetCycleMode(
            appWidgetId = appWidgetId,
            cycleMode = photoWidget.cycleMode,
        )

        val tapActions: PhotoWidgetTapActions = photoWidget.tapActions.coerceTapActions(source = photoWidget.source)

        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = tapActions.left,
            tapActionArea = TapActionArea.LEFT,
        )
        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = tapActions.center,
            tapActionArea = TapActionArea.CENTER,
        )
        photoWidgetStorage.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = tapActions.right,
            tapActionArea = TapActionArea.RIGHT,
        )
    }
}
