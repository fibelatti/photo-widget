package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.backup.RestoreBackupUseCase
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.isInside
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.last

class RestoreWidgetUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(
        originalWidget: PhotoWidget,
        newAppWidgetId: Int,
    ): PhotoWidget {
        require(originalWidget.source == PhotoWidgetSource.PHOTOS || originalWidget.source == PhotoWidgetSource.GIF) {
            "Only photos and GIF widgets can be restored."
        }

        val sourceDir: File? = originalWidget.photos.firstOrNull()
            ?.croppedPhotoPath
            ?.substringBeforeLast("/", missingDelimiterValue = "")
            ?.let(::File)
            ?.takeIf { it.exists() && it.isDirectory }

        requireNotNull(sourceDir) {
            "Cannot restore widget. Source directory is missing."
        }

        require(sourceDir.isInside(RestoreBackupUseCase.extractionDir(context = context))) {
            "Cannot restore widget. Source directory is not an extracted backup."
        }

        photoWidgetStorage.saveWidgetSource(
            appWidgetId = newAppWidgetId,
            source = originalWidget.source,
        )

        photoWidgetStorage.importWidgetDir(
            appWidgetId = newAppWidgetId,
            sourceDir = sourceDir,
        )

        val photos: List<LocalPhoto> = photoWidgetStorage.loadWidgetPhotos(appWidgetId = newAppWidgetId)
            .last()
            .current

        return originalWidget.copy(
            photos = photos,
            currentPhoto = photos.firstOrNull(),
        )
    }
}
