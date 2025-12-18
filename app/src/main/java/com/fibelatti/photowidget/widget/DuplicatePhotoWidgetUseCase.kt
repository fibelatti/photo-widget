package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last

class DuplicatePhotoWidgetUseCase @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(
        originalAppWidgetId: Int,
        newAppWidgetId: Int,
    ): PhotoWidget {
        val appWidget = loadPhotoWidgetUseCase(appWidgetId = originalAppWidgetId).first()

        photoWidgetStorage.saveWidgetSource(
            appWidgetId = newAppWidgetId,
            source = appWidget.source,
        )

        when (appWidget.source) {
            PhotoWidgetSource.PHOTOS -> {
                photoWidgetStorage.duplicateWidgetDir(
                    originalAppWidgetId = originalAppWidgetId,
                    newAppWidgetId = newAppWidgetId,
                )
            }

            PhotoWidgetSource.DIRECTORY -> {
                photoWidgetStorage.saveWidgetSyncedDir(
                    appWidgetId = newAppWidgetId,
                    dirUri = appWidget.syncedDir,
                )
            }
        }

        photoWidgetStorage.copyWidgetOrder(
            sourceWidgetId = originalAppWidgetId,
            targetWidgetId = newAppWidgetId,
        )

        photoWidgetStorage.deletePhotos(
            appWidgetId = newAppWidgetId,
            photoIds = photoWidgetStorage.getExcludedPhotoIds(appWidgetId = originalAppWidgetId),
        )

        val photos = photoWidgetStorage.getWidgetPhotos(appWidgetId = newAppWidgetId)
            .last()
            .current

        return appWidget.copy(
            photos = photos,
            currentPhoto = photos.firstOrNull(),
            deletionTimestamp = -1,
            removedPhotos = emptyList(),
            isLoading = false,
        )
    }
}
