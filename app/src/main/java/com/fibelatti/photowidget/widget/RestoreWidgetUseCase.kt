package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.backup.RestoreBackupUseCase
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.PhotoWidgetTextFont
import com.fibelatti.photowidget.model.copyStyle
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

        val extractionDir: File = RestoreBackupUseCase.extractionDir(context = context)

        require(sourceDir.isInside(extractionDir)) {
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
            text = photoWidgetStorage.stageWidgetFont(
                appWidgetId = newAppWidgetId,
                text = originalWidget.text.withFontInside(dir = extractionDir),
            ),
        )
    }

    /**
     * The name of the custom font comes from the backup, so a font outside of it is dropped like
     * a missing one.
     */
    private fun PhotoWidgetText.withFontInside(dir: File): PhotoWidgetText {
        val customFont: PhotoWidgetTextFont.Custom = font as? PhotoWidgetTextFont.Custom ?: return this

        return if (File(customFont.path).isInside(dir)) this else copyStyle(font = PhotoWidgetTextFont.Default)
    }
}
