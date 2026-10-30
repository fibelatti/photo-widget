package com.fibelatti.photowidget.backup

import android.content.Context
import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.data.PhotoWidgetInternalFileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RestoreBackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zipUtils: ZipUtils,
    private val json: Json,
) {

    suspend operator fun invoke(uri: Uri): Pair<File?, List<PhotoWidget>> = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext null to emptyList()

        val extractedDir: File = inputStream
            .buffered()
            .let(::ZipInputStream)
            .use { zipInputStream ->
                zipUtils.extractZip(
                    zipInputStream = zipInputStream,
                    destinationDir = extractionDir(context = context),
                )
            }

        val jsonFile = File("$extractedDir/widgets.json")

        if (!jsonFile.exists()) {
            extractedDir.deleteRecursively()
            error("Invalid backup file.")
        }

        val exportedWidgets: List<PhotoWidgetExport> = jsonFile.inputStream()
            .reader()
            .use { it.readText() }
            .let(json::decodeFromString)

        return@withContext extractedDir to exportedWidgets.mapNotNull { exported ->
            val photosDir = File("$extractedDir/${exported.id}")
            if (!photosDir.exists()) return@mapNotNull null

            val photoFiles: List<File> = photosDir.listFiles().orEmpty()
                .filterNot { it.isDirectory }
                .ifEmpty { return@mapNotNull null }

            exported.toPhotoWidget(
                photos = photoFiles.map { file: File ->
                    LocalPhoto(
                        photoId = file.name,
                        croppedPhotoPath = file.absolutePath,
                        originalPhotoPath = null, // The dir path will be inferred from the cropped photo path
                    )
                },
                customFontPath = { fileName: String ->
                    File(photosDir, "${PhotoWidgetInternalFileStorage.FONTS_DIR_NAME}/$fileName").path
                },
            )
        }
    }

    companion object {

        /**
         * The only location widgets can be restored from, keeping restores away from the app's other
         * files, some of which are shared through its `FileProvider`.
         */
        fun extractionDir(context: Context): File = context.cacheDir.resolve(relative = "restore")
    }
}
