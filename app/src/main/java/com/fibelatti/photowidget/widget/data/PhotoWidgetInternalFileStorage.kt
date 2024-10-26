package com.fibelatti.photowidget.widget.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.PhotoDecoder
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
    private val decoder: PhotoDecoder,
) {

    private val rootDir by lazy {
        File("${context.filesDir}/widgets").apply {
            mkdirs()
        }
    }

    suspend fun newWidgetPhoto(appWidgetId: Int, source: Uri): LocalPhoto? {
        return withContext(Dispatchers.IO) {
            Timber.d("New widget photo: $source (appWidgetId=$appWidgetId)")
            val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
            val originalPhotosDir = File("$widgetDir/original").apply { mkdirs() }
            val newPhotoName = "${UUID.randomUUID()}.png"

            val originalPhoto = File("$originalPhotosDir/$newPhotoName")
            val croppedPhoto = File("$widgetDir/$newPhotoName")

            val newFiles = listOf(originalPhoto, croppedPhoto)

            runCatching {
                decoder.decode(data = source, maxDimension = PhotoWidget.MAX_STORAGE_DIMENSION)?.let { importedPhoto ->
                    newFiles.map { file ->
                        file.createNewFile()

                        async {
                            FileOutputStream(file).use { fos ->
                                importedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                Timber.d("$source saved to $file")
                            }
                        }
                    }.awaitAll()
                } ?: return@withContext null // Exit early if the bitmap can't be decoded

                // Safety check to ensure the photos were copied correctly
                return@withContext if (newFiles.all { it.exists() }) {
                    LocalPhoto(
                        name = newPhotoName,
                        path = croppedPhoto.path,
                    )
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val croppedPhoto = File("$widgetDir/${localPhoto.name}").apply { createNewFile() }

        if (localPhoto.externalUri != null) {
            return localPhoto.externalUri to Uri.fromFile(croppedPhoto)
        } else {
            val originalPhotosDir = File("$widgetDir/original")
            val originalPhoto = File("$originalPhotosDir/${localPhoto.name}").apply { mkdirs() }

            if (!originalPhoto.exists()) {
                withContext(Dispatchers.IO) {
                    originalPhoto.createNewFile()

                    FileInputStream(croppedPhoto).use { fileInputStream ->
                        fileInputStream.copyTo(FileOutputStream(originalPhoto))
                    }
                }
            }

            return Uri.fromFile(originalPhoto) to Uri.fromFile(croppedPhoto)
        }
    }

    suspend fun deleteWidgetPhoto(appWidgetId: Int, photoName: String) {
        withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
            val originalPhotosDir = File("$widgetDir/original")

            with(File("$originalPhotosDir/$photoName")) {
                if (exists()) delete()
            }
            with(File("$widgetDir/$photoName")) {
                if (exists()) delete()
            }
        }
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        withContext(Dispatchers.IO) {
            getWidgetDir(appWidgetId).deleteRecursively()
        }
    }

    suspend fun getWidgetPhotos(
        appWidgetId: Int,
        source: PhotoWidgetSource,
        originalPhotos: Boolean = false,
    ): List<LocalPhoto> {
        return withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
            val originalPhotosDir = File("$widgetDir/original")
            val originalPhotoNames = originalPhotosDir.list().orEmpty().toSet()

            if (originalPhotos) {
                originalPhotoNames.map { file -> LocalPhoto(name = file, path = "$originalPhotosDir/$file") }
            } else {
                val fileNameFilter = FilenameFilter { _, name ->
                    name != "original" && (name in originalPhotoNames || PhotoWidgetSource.DIRECTORY == source)
                }

                widgetDir.list(fileNameFilter)
                    .orEmpty()
                    .map { file -> LocalPhoto(name = file, path = "$widgetDir/$file") }
            }
        }
    }

    suspend fun getWidgetIds(): List<Int> {
        return withContext(Dispatchers.IO) {
            rootDir.listFiles().orEmpty()
                .filter { it.isDirectory }
                .map { it.name.toInt() }
        }
    }

    fun renameTemporaryWidgetDir(appWidgetId: Int) {
        val tempDir = File("$rootDir/0")
        if (tempDir.exists()) {
            tempDir.renameTo(File("$rootDir/$appWidgetId"))
        }
    }

    suspend fun duplicateWidgetDir(originalAppWidgetId: Int, newAppWidgetId: Int) {
        withContext(Dispatchers.IO) {
            val originalDir = getWidgetDir(appWidgetId = originalAppWidgetId)
            val newDir = getWidgetDir(appWidgetId = newAppWidgetId)

            originalDir.copyRecursively(newDir, overwrite = true)
        }
    }

    private fun getWidgetDir(appWidgetId: Int): File {
        return File("$rootDir/$appWidgetId").apply {
            mkdirs()
        }
    }
}
