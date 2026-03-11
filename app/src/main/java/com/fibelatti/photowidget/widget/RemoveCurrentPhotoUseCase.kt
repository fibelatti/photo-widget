package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject

class RemoveCurrentPhotoUseCase @Inject constructor(
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(appWidgetId: Int): Boolean {
        val currentPhotoId: String = photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId) ?: return false

        if (photoWidgetStorage.getWidgetPhotoCountEstimate(appWidgetId = appWidgetId) <= 1) return false

        when (photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId)) {
            PhotoWidgetSource.PHOTOS -> {
                photoWidgetStorage.appendPhotosForDeletion(
                    appWidgetId = appWidgetId,
                    photoIds = listOf(currentPhotoId),
                )
            }

            PhotoWidgetSource.DIRECTORY -> {
                photoWidgetStorage.appendExcludedPhotos(
                    appWidgetId = appWidgetId,
                    photoIds = listOf(currentPhotoId),
                )
            }
        }

        return true
    }
}
