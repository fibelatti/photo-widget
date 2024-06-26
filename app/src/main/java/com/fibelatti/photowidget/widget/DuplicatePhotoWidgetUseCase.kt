package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidgetSource
import javax.inject.Inject

class DuplicatePhotoWidgetUseCase @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(
        originalAppWidgetId: Int,
        newAppWidgetId: Int,
    ) {
        val appWidget = loadPhotoWidgetUseCase(appWidgetId = originalAppWidgetId)

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
    }
}
