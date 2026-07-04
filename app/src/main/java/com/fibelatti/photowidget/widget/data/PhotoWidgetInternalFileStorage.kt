package com.fibelatti.photowidget.widget.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.fibelatti.photowidget.model.GifFrames
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.platform.ImageFormat
import com.fibelatti.photowidget.platform.ImageFormatDetector
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.UriPermissionGrantor
import com.fibelatti.photowidget.platform.getMaxCrossfadeBitmapDimension
import com.fibelatti.photowidget.platform.runWithFileOutputStream
import com.fibelatti.photowidget.platform.scaledToMaxDimension
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
import pl.droidsonroids.gif.GifDrawable
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
                Timber.d("New widget photo %s", mapOf("source" to source, "directoryName" to directoryName))

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
                                file.runWithFileOutputStream { fos ->
                                    importedPhoto.compress(compressFormat, 95, fos)
                                }
                            }
                        }.awaitAll()
                    }
                } else {
                    newFiles.map { file ->
                        async {
                            contentResolver.openInputStream(source)?.use { input ->
                                file.runWithFileOutputStream(input::copyTo)
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

    suspend fun newWidgetPhotosFromGif(directoryName: String, source: Uri): GifFrames {
        return withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("New widget photos from GIF %s", mapOf("source" to source, "directoryName" to directoryName))

                val widgetDir = getWidgetDir(directoryName = directoryName)
                val originalPhotosDir: File = File("$widgetDir/original").apply { mkdirs() }

                val gifDrawable: GifDrawable = contentResolver.openAssetFileDescriptor(source, "r")
                    ?.let(::GifDrawable)
                    ?: return@withContext GifFrames.EMPTY

                val frameCount: Int = gifDrawable.numberOfFrames
                val duration: Long = gifDrawable.duration.toLong()
                Timber.d("GIF has $frameCount frames, over $duration milliseconds")

                val photos = mutableListOf<LocalPhoto>()

                for (i in 0 until frameCount) {
                    val frameBitmap = gifDrawable.seekToFrameAndGet(i)
                    val newPhotoName = "${UUID.randomUUID()}.png"

                    val originalPhoto = File("$originalPhotosDir/$newPhotoName")
                    val croppedPhoto = File("$widgetDir/$newPhotoName")

                    listOf(originalPhoto, croppedPhoto).forEach { file ->
                        file.runWithFileOutputStream { fos ->
                            frameBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                    }

                    if (originalPhoto.exists() && croppedPhoto.exists()) {
                        photos.add(
                            LocalPhoto(
                                photoId = newPhotoName,
                                croppedPhotoPath = croppedPhoto.path,
                                originalPhotoPath = originalPhoto.path,
                            ),
                        )
                    }
                }

                gifDrawable.recycle()

                return@withContext GifFrames(
                    frames = photos,
                    interval = (duration / frameCount).coerceIn(GifFrames.MIN_INTERVAL_MS, GifFrames.MAX_INTERVAL_MS),
                )
            }.getOrElse { throwable ->
                Timber.e(throwable, "Failed to extract GIF frames")
                GifFrames.EMPTY
            }
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
            val deleteOp: suspend () -> Boolean = {
                getWidgetDir(directoryName).deleteRecursively() &&
                    getCurrentPhotoDir(directoryName).deleteRecursively()
            }
            var count = 1

            while (!deleteOp() && count <= 3) {
                count++
            }
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
                name != "original" && (name in originalPhotoNames || source == PhotoWidgetSource.DIRECTORY)
            }

            widgetDir.list(fileNameFilter)
                .orEmpty()
                .map { fileName ->
                    val croppedPhotoPath = "$widgetDir/$fileName"

                    LocalPhoto(
                        photoId = fileName,
                        croppedPhotoPath = croppedPhotoPath,
                        originalPhotoPath = "$originalPhotosDir/$fileName",
                        launcherUri = if (source == PhotoWidgetSource.GIF) {
                            uriPermissionGrantor(path = croppedPhotoPath)
                        } else {
                            null
                        },
                    )
                }
        }
    }

    suspend fun prepareCurrentWidgetPhoto(
        directoryName: String,
        currentPhoto: Bitmap,
        crossfadeIntent: Boolean,
    ): PreparedCurrentPhoto = withContext(Dispatchers.IO) {
        val dir: File = getCurrentPhotoDir(directoryName = directoryName)

        // The newest existing file is the last photo persisted; keep it as the "previous" image
        // for a crossfade transition and drop anything older. It reflects the photo currently
        // on screen only while persistence keeps running (crossfade enabled or recovery mode).
        // If photos change while these conditions are not true, nothing is persisted. On
        // re-enabling crossfade, the first transition may start from a stale previous, which
        // self-heals on the next iteration. Only `.webp` files count — a stray `.tmp` is an
        // interrupted write (see the atomic write below); ignore and delete those so a truncated
        // file is never picked as the previous.
        val existing: List<File> = dir.listFiles()?.toList().orEmpty()
        existing.filter { it.name.endsWith(TMP_SUFFIX) }.forEach { it.delete() }
        val previousFile: File? = existing.filter { it.name.endsWith(WEBP_SUFFIX) }.sortedBy { it.name }
            .also { photos -> photos.dropLast(1).forEach { it.delete() } }
            .lastOrNull()

        // Using `currentTimeMillis` to generate unique files, otherwise the widget won't update if
        // the same file is overwritten every time
        val file = File("$dir/${System.currentTimeMillis()}$WEBP_SUFFIX")

        // Build the crossfade source bitmaps only when the caller is setting up a fade.
        // Both the incoming photo and the decoded previous one are capped at
        // getMaxCrossfadeBitmapDimension so the two together fit a single RemoteViews update.
        // The widget still settles on the full-resolution `bitmap`, so this downscale only softens
        // the ~1s fade, not the resting image.
        if (crossfadeIntent) {
            val fadeDimension: Int = context.getMaxCrossfadeBitmapDimension()
            val fadeBitmap: Bitmap = currentPhoto.scaledToMaxDimension(fadeDimension)
            val previousBitmap: Bitmap? = previousFile?.let { existingFile ->
                decoder.decode(data = existingFile.path, maxDimension = fadeDimension)
            }

            // The crossfade renders entirely from the in-memory bitmaps above and never reads this
            // file, so the WebP encode+write is only needed to seed the next iteration. Hand it
            // back as a deferred action the caller runs concurrently with the fade instead of
            // before it.
            return@withContext PreparedCurrentPhoto(
                bitmap = currentPhoto,
                previousBitmap = previousBitmap,
                fadeBitmap = fadeBitmap,
                pendingWrite = { writeWidgetPhoto(bitmap = currentPhoto, file = file) },
            )
        }

        // Recovery/plain persist: the render uses the URI, so the file must exist before returning.
        writeWidgetPhoto(bitmap = currentPhoto, file = file)

        return@withContext PreparedCurrentPhoto(
            bitmap = currentPhoto,
            uri = uriPermissionGrantor(path = file.path),
        )
    }

    /**
     * Atomically persists [bitmap] to [file]: encodes to a sibling `.tmp` then renames it into place,
     * so a reader (e.g. the next tap selecting the previous photo) only ever sees a fully written
     * file, never a half-encoded one, even if the process dies mid-write. Rename is atomic within
     * the same directory. WebP-lossy encodes faster than lossless PNG for this transformed bitmap,
     * and retains the alpha channel needed by rounded corners and polygonal shapes.
     */
    private suspend fun writeWidgetPhoto(bitmap: Bitmap, file: File) {
        withContext(Dispatchers.IO) {
            val tempFile = File(file.path + TMP_SUFFIX)
            tempFile.runWithFileOutputStream { fos ->
                bitmap.compress(webpLossyFormat(), WIDGET_PHOTO_QUALITY, fos)
            }
            if (!tempFile.renameTo(file)) {
                tempFile.delete()
                error("Failed to persist widget photo ${file.name}")
            }
        }
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

    private fun webpLossyFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            // WEBP_LOSSY requires API 30; WEBP is the lossy WebP format on API 26-29.
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private companion object {

        // Max-quality lossy: visually indistinguishable from the source yet still encodes faster
        // than lossless PNG (and faster than lossless WebP, which is slower than PNG).
        // The render cache is the only lossy step throughout processing.
        private const val WIDGET_PHOTO_QUALITY: Int = 100

        private const val WEBP_SUFFIX: String = ".webp"

        // Suffix for the in-progress atomic write; renamed to its `.webp` target once fully encoded.
        private const val TMP_SUFFIX: String = ".tmp"
    }
}
