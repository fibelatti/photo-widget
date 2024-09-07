package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.last

class DuplicatePhotoWidgetUseCase @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(
        originalAppWidgetId: Int,
        newAppWidgetId: Int,
    ): PhotoWidget {
        val appWidget = loadPhotoWidgetUseCase(appWidgetId = originalAppWidgetId).last()

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

        return appWidget.copy(
            currentIndex = 0,
            deletionTimestamp = -1,
        )
    }
}
