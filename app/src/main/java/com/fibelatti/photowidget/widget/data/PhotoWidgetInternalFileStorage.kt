package com.fibelatti.photowidget.widget.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.ImageFormat
import com.fibelatti.photowidget.platform.ImageFormatDetector
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.UriPermissionGrantor
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber

class PhotoWidgetInternalFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesStorage: UserPreferencesStorage,
    private val imageFormatDetector: ImageFormatDetector,
    private val decoder: PhotoDecoder,
    private val uriPermissionGrantor: UriPermissionGrantor,
) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val rootDir: File by lazy {
        File("${context.filesDir}/widgets").apply {
            mkdirs()
        }
    }

    suspend fun newWidgetPhoto(directoryName: String, source: Uri): LocalPhoto? {
        return withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("New widget photo: $source (directoryName=$directoryName)")

                val widgetDir: File = getWidgetDir(directoryName = directoryName)
                val originalPhotosDir: File = File("$widgetDir/original").apply { mkdirs() }

                val format: ImageFormat = imageFormatDetector.getImageFormat(context = context, imageUri = source)
                val extension: String = if (format == ImageFormat.JPEG) "jpg" else "png"
                val newPhotoName = "${UUID.randomUUID()}.$extension"

                val originalPhoto = File("$originalPhotosDir/$newPhotoName")
                val croppedPhoto = File("$widgetDir/$newPhotoName")

                val newFiles: List<File> = listOf(originalPhoto, croppedPhoto)
                val dataSaver: Boolean = userPreferencesStorage.dataSaver

                Timber.d("Data saver: $dataSaver")

                if (dataSaver) {
                    decoder.decode(data = source, maxDimension = 2560)?.let { importedPhoto ->
                        val compressFormat: Bitmap.CompressFormat = if (format == ImageFormat.JPEG) {
                            Bitmap.CompressFormat.JPEG
                        } else {
                            Bitmap.CompressFormat.PNG
                        }
                        newFiles.map { file ->
                            async {
                                writeToFile(file) { fos -> importedPhoto.compress(compressFormat, 95, fos) }
                            }
                        }.awaitAll()
                    }
                } else {
                    newFiles.map { file ->
                        async {
                            contentResolver.openInputStream(source)?.use { input ->
                                writeToFile(file, input::copyTo)
                            }
                        }
                    }.awaitAll()
                }

                return@withContext if (newFiles.all { it.exists() }) {
                    LocalPhoto(
                        photoId = newPhotoName,
                        croppedPhotoPath = croppedPhoto.path,
                        originalPhotoPath = originalPhoto.path,
                    )
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    private fun writeToFile(file: File, operation: (FileOutputStream) -> Unit) {
        if (!file.exists()) file.createNewFile()
        runCatching {
            FileOutputStream(file).use { fos -> operation(fos) }
            require(file.length() > 0) { "File is empty" }
        }.onSuccess {
            Timber.d("Successfully saved to $file")
        }.onFailure {
            Timber.e(it, "Failed to save to $file")
            file.delete()
        }
    }

    suspend fun getCropSources(
        directoryName: String,
        localPhoto: LocalPhoto,
    ): Pair<Uri, Uri> = withContext(Dispatchers.IO) {
        val widgetDir = getWidgetDir(directoryName = directoryName)
        val croppedPhoto = File("$widgetDir/${localPhoto.photoId}").apply { createNewFile() }

        return@withContext if (localPhoto.externalUri != null) {
            localPhoto.externalUri to Uri.fromFile(croppedPhoto)
        } else {
            val originalPhotosDir = File("$widgetDir/original")
            val originalPhoto = File("$originalPhotosDir/${localPhoto.photoId}").apply { mkdirs() }

            if (!originalPhoto.exists()) {
                originalPhoto.createNewFile()

                FileInputStream(croppedPhoto).use { fileInputStream ->
                    fileInputStream.copyTo(FileOutputStream(originalPhoto))
                }
            }

            Uri.fromFile(originalPhoto) to Uri.fromFile(croppedPhoto)
        }
    }

    suspend fun deleteWidgetPhoto(directoryName: String, photoId: String) {
        withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(directoryName = directoryName)
            val originalPhotosDir = File("$widgetDir/original")

            with(File("$originalPhotosDir/$photoId")) {
                if (exists()) delete()
            }
            with(File("$widgetDir/$photoId")) {
                if (exists()) delete()
            }
        }
    }

    suspend fun deleteWidgetData(directoryName: String) {
        withContext(Dispatchers.IO) {
            getWidgetDir(directoryName).deleteRecursively()
            getCurrentPhotoDir(directoryName).deleteRecursively()
        }
    }

    suspend fun getWidgetPhotos(
        directoryName: String,
        source: PhotoWidgetSource,
    ): List<LocalPhoto> {
        return withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(directoryName = directoryName)
            val originalPhotosDir = File("$widgetDir/original")
            val originalPhotoNames = originalPhotosDir.list().orEmpty().toSet()

            val fileNameFilter = FilenameFilter { _, name ->
                name != "original" && (name in originalPhotoNames || PhotoWidgetSource.DIRECTORY == source)
            }

            widgetDir.list(fileNameFilter)
                .orEmpty()
                .map { fileName ->
                    LocalPhoto(
                        photoId = fileName,
                        croppedPhotoPath = "$widgetDir/$fileName",
                        originalPhotoPath = "$originalPhotosDir/$fileName",
                    )
                }
        }
    }

    suspend fun prepareCurrentWidgetPhoto(
        directoryName: String,
        currentPhoto: Bitmap,
    ): Uri? = withContext(Dispatchers.IO) {
        val dir: File = getCurrentPhotoDir(directoryName = directoryName).apply {
            listFiles()?.toList()?.sortedBy { it.name }?.dropLast(1)?.forEach { it.delete() }
        }
        // Using `currentTimeMillis` to generate unique files,
        // otherwise the widget won't update if the same file is overwritten every time
        val file = File("$dir/${System.currentTimeMillis()}.png")

        writeToFile(file) { fos ->
            currentPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        return@withContext uriPermissionGrantor(path = file.path)
    }

    suspend fun duplicateWidgetDir(sourceDirectoryName: String, targetDirectoryName: String) {
        withContext(Dispatchers.IO) {
            val originalDir = getWidgetDir(directoryName = sourceDirectoryName)
            val newDir = getWidgetDir(directoryName = targetDirectoryName)

            originalDir.copyRecursively(newDir, overwrite = true)
        }
    }

    private suspend fun getWidgetDir(directoryName: String): File = withContext(Dispatchers.IO) {
        File("$rootDir/$directoryName").apply {
            mkdirs()
        }
    }

    private suspend fun getCurrentPhotoDir(directoryName: String): File = withContext(Dispatchers.IO) {
        File("$rootDir/current_photos/$directoryName").apply {
            mkdirs()
        }
    }

    suspend fun exportWidgetDir(directoryName: String, appWidgetId: Int, destinationDir: File) {
        withContext(Dispatchers.IO) {
            getWidgetDir(directoryName = directoryName)
                .copyRecursively(target = File("$destinationDir/$appWidgetId"))
        }
    }

    suspend fun importWidgetDir(directoryName: String, sourceDir: File) {
        withContext(Dispatchers.IO) {
            sourceDir.copyRecursively(target = getWidgetDir(directoryName = directoryName))
        }
    }

    companion object {

        const val DRAFT_WIDGET_ID = 0
    }
}
