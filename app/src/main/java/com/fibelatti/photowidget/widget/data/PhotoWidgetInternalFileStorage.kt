package com.fibelatti.photowidget.widget.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.PhotoDecoder
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
    private val decoder: PhotoDecoder,
) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val mimeTypeMap: MimeTypeMap = MimeTypeMap.getSingleton()
    private val rootDir: File by lazy {
        File("${context.filesDir}/widgets").apply {
            mkdirs()
        }
    }

    suspend fun newWidgetPhoto(appWidgetId: Int, source: Uri): LocalPhoto? {
        return withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("New widget photo: $source (appWidgetId=$appWidgetId)")

                val widgetDir: File = getWidgetDir(appWidgetId = appWidgetId)
                val originalPhotosDir: File = File("$widgetDir/original").apply { mkdirs() }
                val extension: String = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(source))
                    ?.lowercase()
                    ?: "png"
                val newPhotoName = "${UUID.randomUUID()}.$extension"

                val originalPhoto = File("$originalPhotosDir/$newPhotoName")
                val croppedPhoto = File("$widgetDir/$newPhotoName")

                val newFiles: List<File> = listOf(originalPhoto, croppedPhoto)
                val dataSaver: Boolean = userPreferencesStorage.dataSaver

                Timber.d("Data saver: $dataSaver")

                if (dataSaver) {
                    decoder.decode(data = source, maxDimension = 2560)?.let { importedPhoto ->
                        val format: Bitmap.CompressFormat = if (extension == "png") {
                            Bitmap.CompressFormat.PNG
                        } else {
                            Bitmap.CompressFormat.JPEG
                        }
                        newFiles.map { file ->
                            async {
                                writeToFile(file) { fos -> importedPhoto.compress(format, 95, fos) }
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
        file.createNewFile()
        runCatching {
            FileOutputStream(file).use { fos -> operation(fos) }
            require(file.length() > 0) { "File is empty" }
        }.onSuccess {
            Timber.d("Successfully saved to $file")
        }.onFailure {
            Timber.d("Failed to save to $file")
            file.delete()
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> = withContext(Dispatchers.IO) {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
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

    suspend fun deleteWidgetPhoto(appWidgetId: Int, photoId: String) {
        withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
            val originalPhotosDir = File("$widgetDir/original")

            with(File("$originalPhotosDir/$photoId")) {
                if (exists()) delete()
            }
            with(File("$widgetDir/$photoId")) {
                if (exists()) delete()
            }
        }
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        withContext(Dispatchers.IO) {
            getWidgetDir(appWidgetId).deleteRecursively()
            getCurrentPhotoDir(appWidgetId).deleteRecursively()
        }
    }

    suspend fun getWidgetPhotos(
        appWidgetId: Int,
        source: PhotoWidgetSource,
    ): List<LocalPhoto> {
        return withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
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
        appWidgetId: Int,
        currentPhoto: Bitmap,
    ): Uri? = withContext(Dispatchers.IO) {
        val launcherPackages: List<String> = getLauncherPackages()
        if (launcherPackages.isEmpty()) {
            Timber.d("No launcher packages found, unable to generate URI.")
            return@withContext null
        }

        val dir: File = getCurrentPhotoDir(appWidgetId = appWidgetId).apply {
            listFiles()?.toList()?.sortedBy { it.name }?.dropLast(1)?.forEach { it.delete() }
        }
        // Using `currentTimeMillis` to generate unique files,
        // otherwise the widget won't update if the same file is overwritten every time
        val file = File("$dir/${System.currentTimeMillis()}.png")

        writeToFile(file) { fos ->
            currentPhoto.compress(Bitmap.CompressFormat.PNG, 0, fos)
        }

        if (!file.exists()) {
            Timber.d("Failed to create file, unable to generate URI.")
            return@withContext null
        }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Timber.d("New URI for widget with id $appWidgetId: $uri")

        val packages: List<String> = launcherPackages
            .plus(
                if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                    samsungPackages
                } else {
                    emptyList()
                },
            )

        for (pkg in packages) {
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Timber.d("Granted URI permission for package: $pkg")
        }

        return@withContext uri
    }

    private fun getLauncherPackages(): List<String> {
        val intent = Intent("android.intent.action.MAIN")
            .addCategory("android.intent.category.HOME")

        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.packageName }
            .distinct()
    }

    suspend fun renameTemporaryWidgetDir(appWidgetId: Int) {
        withContext(Dispatchers.IO) {
            val tempDir = File("$rootDir/0")
            if (tempDir.exists()) {
                tempDir.renameTo(File("$rootDir/$appWidgetId"))
            }
        }
    }

    suspend fun duplicateWidgetDir(originalAppWidgetId: Int, newAppWidgetId: Int) {
        withContext(Dispatchers.IO) {
            val originalDir = getWidgetDir(appWidgetId = originalAppWidgetId)
            val newDir = getWidgetDir(appWidgetId = newAppWidgetId)

            originalDir.copyRecursively(newDir, overwrite = true)
        }
    }

    private suspend fun getWidgetDir(appWidgetId: Int): File = withContext(Dispatchers.IO) {
        File("$rootDir/$appWidgetId").apply {
            mkdirs()
        }
    }

    private suspend fun getCurrentPhotoDir(appWidgetId: Int): File = withContext(Dispatchers.IO) {
        File("$rootDir/current_photos/$appWidgetId").apply {
            mkdirs()
        }
    }

    suspend fun exportWidgetDir(appWidgetId: Int, destinationDir: File) {
        withContext(Dispatchers.IO) {
            getWidgetDir(appWidgetId = appWidgetId)
                .copyRecursively(target = File("$destinationDir/$appWidgetId"))
        }
    }

    suspend fun importWidgetDir(appWidgetId: Int, sourceDir: File) {
        withContext(Dispatchers.IO) {
            sourceDir.copyRecursively(target = getWidgetDir(appWidgetId = appWidgetId))
        }
    }

    private companion object {

        val samsungPackages: List<String> = listOf(
            "com.samsung.android.goodlock",
            "com.samsung.systemui.lockstar",
        )
    }
}
