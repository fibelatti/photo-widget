package com.fibelatti.photowidget.widget.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.annotation.ColorInt
import com.fibelatti.photowidget.model.GifFrames
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTextFont
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.platform.ImageFormat
import com.fibelatti.photowidget.platform.ImageFormatDetector
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.TextFontLoader
import com.fibelatti.photowidget.platform.UriPermissionGrantor
import com.fibelatti.photowidget.platform.getMaxCrossfadeBitmapDimension
import com.fibelatti.photowidget.platform.runWithFileOutputStream
import com.fibelatti.photowidget.platform.scaledToMaxDimension
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
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

    /**
     * Fonts chosen while configuring a widget, kept apart from its directory until the widget is
     * saved, see [saveWidgetFont]. Each one has a directory of its own, so files with the same name
     * don't replace each other.
     */
    private val fontStagingRootDir: File by lazy {
        File("${context.noBackupFilesDir}/font_staging")
    }

    suspend fun newWidgetPhoto(directoryName: String, source: Uri): LocalPhoto? {
        return withContext(Dispatchers.IO) {
            runCatching {
                Timber.d("New widget photo %s", mapOf("directoryName" to directoryName, "source" to source))

                val widgetDir: File = getWidgetDir(directoryName = directoryName)
                val format: ImageFormat = imageFormatDetector.getImageFormat(context = context, imageUri = source)
                val extension: String = if (format == ImageFormat.JPEG) "jpg" else "png"
                val newPhotoName = "${UUID.randomUUID()}.$extension"
                val dataSaver: Boolean = userPreferencesStorage.dataSaver

                Timber.d("Data saver: $dataSaver")

                newLocalPhoto(widgetDir = widgetDir, photoName = newPhotoName) { files: List<File> ->
                    if (dataSaver) {
                        decoder.decode(data = source, maxDimension = 2560)?.let { importedPhoto: Bitmap ->
                            val compressFormat: Bitmap.CompressFormat = if (format == ImageFormat.JPEG) {
                                Bitmap.CompressFormat.JPEG
                            } else {
                                Bitmap.CompressFormat.PNG
                            }
                            coroutineScope {
                                files.map { file ->
                                    async {
                                        file.runWithFileOutputStream { fos ->
                                            importedPhoto.compress(compressFormat, 95, fos)
                                        }
                                    }
                                }.awaitAll()
                            }
                        }
                    } else {
                        coroutineScope {
                            files.map { file ->
                                async {
                                    contentResolver.openInputStream(source)?.use { input ->
                                        file.runWithFileOutputStream(input::copyTo)
                                    }
                                }
                            }.awaitAll()
                        }
                    }
                }
            }.onFailure { ensureActive() }.getOrNull()
        }
    }

    suspend fun newWidgetPhotosFromGif(directoryName: String, source: Uri): GifFrames {
        return withContext(Dispatchers.IO) {
            Timber.d("New widget photos from GIF %s", mapOf("directoryName" to directoryName, "source" to source))

            val widgetDir: File = getWidgetDir(directoryName = directoryName)
            val photos: MutableList<LocalPhoto> = mutableListOf()

            runCatching {
                val gifDrawable: GifDrawable = contentResolver.openAssetFileDescriptor(source, "r")
                    ?.let(::GifDrawable)
                    ?: return@withContext GifFrames.EMPTY

                try {
                    val frameCount: Int = gifDrawable.numberOfFrames
                    val duration: Long = gifDrawable.duration.toLong()
                    Timber.d("GIF has $frameCount frames, over $duration milliseconds")

                    for (i in 0 until frameCount) {
                        val frameBitmap: Bitmap = gifDrawable.seekToFrameAndGet(i)

                        val photo: LocalPhoto? = newLocalPhoto(
                            widgetDir = widgetDir,
                            photoName = "${UUID.randomUUID()}.png",
                        ) { files ->
                            files.forEach { file ->
                                file.runWithFileOutputStream { fos ->
                                    frameBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                }
                            }
                        }

                        if (photo != null) {
                            photos.add(photo)
                        }
                    }

                    GifFrames(
                        frames = photos,
                        interval = (duration / frameCount).coerceIn(
                            GifFrames.MIN_INTERVAL_MS,
                            GifFrames.MAX_INTERVAL_MS,
                        ),
                    )
                } finally {
                    gifDrawable.recycle()
                }
            }.getOrElse { throwable ->
                for ((photoId: String) in photos) {
                    File(getOriginalPhotosDir(widgetDir), photoId).delete()
                    File(widgetDir, photoId).delete()
                }

                ensureActive()

                Timber.e(throwable, "Failed to extract GIF frames")

                GifFrames.EMPTY
            }
        }
    }

    /**
     * Creates the backing files for a photo named [photoName] under [widgetDir], with [write]
     * writing its original and cropped photos. Both are written to temporary files and only moved
     * into their final destination once both are complete. A failed or interrupted operation
     * leaves no corrupted data behind.
     *
     * @return the new [photo][LocalPhoto], or null if [write] couldn't complete
     */
    private suspend fun newLocalPhoto(
        widgetDir: File,
        photoName: String,
        write: suspend (files: List<File>) -> Unit,
    ): LocalPhoto? {
        val originalPhotosDir: File = getOriginalPhotosDir(widgetDir).apply { mkdirs() }
        val originalPhoto = File(originalPhotosDir, photoName)
        val croppedPhoto = File(widgetDir, photoName)

        val tempOriginalPhoto: File = newInProgressFile(directory = widgetDir)
        val tempCroppedPhoto: File = newInProgressFile(directory = widgetDir)

        try {
            write(listOf(tempOriginalPhoto, tempCroppedPhoto))

            if (tempOriginalPhoto.length() == 0L || tempCroppedPhoto.length() == 0L) return null

            // The cropped photo is moved first since without its original it is not listed or restored
            check(tempCroppedPhoto.renameTo(croppedPhoto)) { "Failed to rename ${tempCroppedPhoto.name}" }
            if (!tempOriginalPhoto.renameTo(originalPhoto)) {
                croppedPhoto.delete()
                error("Failed to rename ${tempOriginalPhoto.name}")
            }

            return LocalPhoto(
                photoId = photoName,
                croppedPhotoPath = croppedPhoto.path,
                originalPhotoPath = originalPhoto.path,
            )
        } finally {
            tempOriginalPhoto.delete()
            tempCroppedPhoto.delete()
        }
    }

    /**
     * Creates a temporary file in [directory] for importing or restoring operations.
     *
     * Leftover files resulting from an interruption are handled by [deleteInterruptedWritesBefore].
     */
    private fun newInProgressFile(directory: File): File {
        return File.createTempFile(
            /* prefix = */ IN_PROGRESS_PREFIX,
            /* suffix = */ TMP_SUFFIX,
            /* directory = */ directory,
        )
    }

    private fun isInProgressFile(name: String): Boolean {
        return name.startsWith(IN_PROGRESS_PREFIX) && name.endsWith(TMP_SUFFIX)
    }

    suspend fun getCropSources(
        directoryName: String,
        localPhoto: LocalPhoto,
    ): Pair<Uri, Uri> = withContext(Dispatchers.IO) {
        val widgetDir = getWidgetDir(directoryName = directoryName)
        val croppedPhoto = File("$widgetDir/${localPhoto.photoId}")

        return@withContext if (localPhoto.externalUri != null) {
            localPhoto.externalUri to Uri.fromFile(croppedPhoto)
        } else {
            val originalPhotosDir = getOriginalPhotosDir(widgetDir).apply { mkdirs() }
            val originalPhoto = File("$originalPhotosDir/${localPhoto.photoId}")

            if (!originalPhoto.exists()) {
                originalPhoto.createNewFile()

                FileInputStream(croppedPhoto).use { fileInputStream ->
                    FileOutputStream(originalPhoto).use(fileInputStream::copyTo)
                }
            }

            Uri.fromFile(originalPhoto) to Uri.fromFile(croppedPhoto)
        }
    }

    suspend fun saveCroppedPhoto(sourcePath: String, destinationPath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Files.move(Paths.get(sourcePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING)
        }.onFailure {
            Timber.e(it, "Failed to save the cropped photo")
            File(sourcePath).delete()
        }.isSuccess
    }

    suspend fun isFile(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).isFile
    }

    /**
     * Recreates the cropped photo at [croppedPhotoPath] as a copy of the original photo, as if it
     * has been just imported. The copy is written to a file of its own and renamed into place, so
     * neither an interrupted copy nor a concurrent restore of the same photo leaves a truncated
     * photo behind.
     *
     * A photo can be deleted while it is being restored. Whatever deletes photo files must delete
     * the original before listing or deleting the cropped sibling, and the restore checks the
     * original again once the copy is in place, removing the copy if the original is gone. Either
     * the copy is in place before the cropped photos are listed, and is deleted with them, or the
     * original is already gone when it is checked again.
     *
     * @return true if the cropped photo was restored
     */
    suspend fun restoreCroppedPhoto(originalPhotoPath: String, croppedPhotoPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            restoreCroppedPhoto(originalPhoto = File(originalPhotoPath), croppedPhoto = File(croppedPhotoPath))
        }
    }

    private fun restoreCroppedPhoto(originalPhoto: File, croppedPhoto: File): Boolean {
        val restored: Boolean = runCatching {
            val tempFile: File = newInProgressFile(directory = checkNotNull(croppedPhoto.parentFile))

            runCatching {
                originalPhoto.copyTo(target = tempFile, overwrite = true)
                check(tempFile.renameTo(croppedPhoto)) {
                    "Failed to rename ${tempFile.name} when restoring"
                }
            }.onFailure { tempFile.delete() }.getOrThrow()
        }.onFailure {
            Timber.e(it, "Failed to restore the cropped photo")
        }.isSuccess

        if (!restored) return false
        if (originalPhoto.exists()) return true

        Timber.d("Photo deleted while restoring its cropped photo %s", croppedPhoto.name)
        croppedPhoto.delete()

        return false
    }

    /**
     * Deletes the files left in the widget directories by import and restore attempts that were
     * interrupted before moving them into place, see [newInProgressFile], last modified before
     * [timestamp].
     */
    suspend fun deleteInterruptedWritesBefore(timestamp: Long) {
        withContext(Dispatchers.IO) {
            for (widgetDir in rootDir.listFiles().orEmpty()) {
                widgetDir.listFiles().orEmpty()
                    .filter { file -> isInProgressFile(name = file.name) && file.lastModified() < timestamp }
                    .forEach { it.delete() }
            }
        }
    }

    suspend fun deleteWidgetPhoto(directoryName: String, photoId: String) {
        withContext(Dispatchers.IO) {
            val widgetDir = getWidgetDir(directoryName = directoryName)
            val originalPhotosDir = getOriginalPhotosDir(widgetDir)

            with(File("$originalPhotosDir/$photoId")) {
                if (exists()) delete()
            }
            with(File("$widgetDir/$photoId")) {
                if (exists()) delete()
            }
        }
    }

    /**
     * Deletes the files of the widget. [keepFonts] keeps its custom font, for when only its photos
     * are being replaced.
     */
    suspend fun deleteWidgetData(directoryName: String, keepFonts: Boolean = false) {
        withContext(Dispatchers.IO) {
            val deleteOp: suspend () -> Boolean = {
                val widgetDir: File = getWidgetDir(directoryName)
                val originalPhotosDeleted: Boolean = getOriginalPhotosDir(widgetDir).deleteRecursively()
                val widgetDirDeleted: Boolean = originalPhotosDeleted && if (keepFonts) {
                    widgetDir.listFiles().orEmpty()
                        .filterNot { it.name == FONTS_DIR_NAME }
                        .all { it.deleteRecursively() }
                } else {
                    widgetDir.deleteRecursively()
                }

                widgetDirDeleted && getCurrentPhotoDir(directoryName).deleteRecursively()
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
            val widgetDir: File = getWidgetDir(directoryName = directoryName)
            val originalPhotosDir: File = getOriginalPhotosDir(widgetDir)
            val originalPhotoNames: Set<String> = originalPhotosDir.list().orEmpty().toSet()

            if (source != PhotoWidgetSource.DIRECTORY) {
                restoreMissingCroppedPhotos(
                    widgetDir = widgetDir,
                    originalPhotosDir = originalPhotosDir,
                    originalPhotoNames = originalPhotoNames,
                )
            }

            val fileNameFilter = FilenameFilter { _, name ->
                name != ORIGINAL_DIR_NAME && name != FONTS_DIR_NAME && !isInProgressFile(name = name) &&
                    (name in originalPhotoNames || source == PhotoWidgetSource.DIRECTORY)
            }

            widgetDir.list(fileNameFilter)
                .orEmpty()
                // Decides which listed files can be deleted: an empty crop without an original is a folder
                // photo that falls back to its source file. Photos with an original are kept, since the
                // filter above lets them through and deleting their crop would drop them from the widget.
                .filterNot { fileName ->
                    fileName !in originalPhotoNames && deleteIfEmpty(File("$widgetDir/$fileName"))
                }
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

    /**
     * Restores the cropped photos missing from [widgetDir] from their originals, so a photo that
     * lost only its cropped file is still listed instead of dropped from the widget.
     */
    private fun restoreMissingCroppedPhotos(widgetDir: File, originalPhotosDir: File, originalPhotoNames: Set<String>) {
        val missingPhotoNames: Set<String> = originalPhotoNames - widgetDir.list().orEmpty().toSet()

        for (photoName in missingPhotoNames) {
            val croppedPhoto = File(widgetDir, photoName)
            val originalPhoto = File(originalPhotosDir, photoName)
            if (!originalPhoto.isFile) continue

            if (restoreCroppedPhoto(originalPhoto = originalPhoto, croppedPhoto = croppedPhoto)) {
                Timber.w("Cropped photo not found, restored from the original photo %s", mapOf("photoId" to photoName))
            }
        }
    }

    /**
     * A failed crop could result in an empty file, which can't be decoded.
     *
     * Folder photos fall back to their source file when there's no cropped version, so an empty
     * cropped file is removed instead of listed.
     *
     * @return true if [file] is empty
     */
    private fun deleteIfEmpty(file: File): Boolean {
        if (!file.isFile || file.length() > 0) return false

        Timber.d("Deleting empty cropped photo %s", file.path)
        file.delete()

        return true
    }

    suspend fun prepareCurrentWidgetPhoto(
        appWidgetId: Int,
        directoryName: String,
        currentPhoto: Bitmap,
        @ColorInt labelColor: Int,
        crossfadeIntent: Boolean,
        labelBytes: Long = 0,
        coerceToWidgetSize: Boolean = true,
    ): PreparedCurrentPhoto = withContext(Dispatchers.IO) {
        val dir: File = getCurrentPhotoDir(directoryName = directoryName)

        // The newest existing file is the last photo persisted; keep it as the "previous" image
        // for a crossfade transition and drop anything older. Renders that put a new photo up
        // without persisting it invalidate this cache (see invalidateCurrentPhotoCache), so a
        // present file always reflects the photo currently on screen — the "previous" is either
        // fresh or absent, never stale. When absent, the caller falls back to a plain swap that
        // reseeds the cache for the next transition. Only `.webp` files count — a stray `.tmp` is
        // an interrupted write (see the atomic write below); ignore and delete those so a truncated
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
        // getMaxCrossfadeBitmapDimension so the two together, plus the label, fit a single
        // RemoteViews update. The widget still settles on the full-resolution bitmap, so this
        // downscale only softens the ~1s fade, not the final widget image.
        if (crossfadeIntent) {
            val fadeDimension: Int = context.getMaxCrossfadeBitmapDimension(
                appWidgetId = appWidgetId,
                labelBytes = labelBytes,
                coerceToWidgetSize = coerceToWidgetSize,
            )
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
                labelColor = labelColor,
                previousBitmap = previousBitmap,
                fadeBitmap = fadeBitmap,
                pendingWrite = { writeWidgetPhoto(bitmap = currentPhoto, file = file) },
            )
        }

        // Recovery branch: the render uses the URI, so the file must exist before returning.
        // A write failure here is a genuine dead-end so the caller must surface the error.
        writeWidgetPhoto(bitmap = currentPhoto, file = file)

        return@withContext PreparedCurrentPhoto(
            bitmap = currentPhoto,
            labelColor = labelColor,
            uri = uriPermissionGrantor(path = file.path),
        )
    }

    /**
     * Drops the retained render cache for [directoryName] so the next crossfade sees no previous
     * photo and does a plain swap that reseeds it, instead of fading from a photo no longer on
     * screen. Called for renders that put a new photo up without persisting it, which would
     * otherwise leave the cache pointing at a stale photo. Does not create the directory: a widget
     * that never crossfaded has no cache, so this is a cheap no-op for it.
     */
    suspend fun invalidateCurrentPhotoCache(directoryName: String) {
        withContext(Dispatchers.IO) {
            val dir = File("$rootDir/current_photos/$directoryName")
            if (!dir.exists()) return@withContext

            dir.listFiles()
                ?.filter { it.name.endsWith(WEBP_SUFFIX) || it.name.endsWith(TMP_SUFFIX) }
                ?.forEach { it.delete() }
        }
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

    /**
     * Copies the font file in [source] to the staging directory of the widget, under its original
     * name. It is copied to the widget directory once the widget is saved, see [saveWidgetFont].
     *
     * @return the staged font, or `null` when [source] isn't a font file the system can load
     */
    suspend fun newWidgetFont(directoryName: String, source: Uri): PhotoWidgetTextFont? = withContext(Dispatchers.IO) {
        runCatching {
            Timber.d("New widget font %s", mapOf("directoryName" to directoryName, "source" to source))

            val hasFontSignature: Boolean = contentResolver.openInputStream(source)
                ?.use(TextFontLoader::hasFontSignature)
                ?: error("Unable to open $source")

            if (!hasFontSignature) {
                Timber.w("Not a font file %s", mapOf("source" to source))
                return@runCatching null
            }

            val fileName: String = getFileName(uri = source)
            val stagingDir: File = newFontStagingDir(directoryName = directoryName)
            val fontFile = File(stagingDir, fileName)

            contentResolver.openInputStream(source)
                ?.use { input -> fontFile.runWithFileOutputStream(input::copyTo) }
                ?: error("Unable to open $source")

            if (TextFontLoader.createTypeface(file = fontFile) == null) {
                Timber.w("Not a valid font file %s", mapOf("source" to source))
                stagingDir.deleteRecursively()
                return@runCatching null
            }

            PhotoWidgetTextFont.Custom(fileName = fileName, path = fontFile.path)
        }.onFailure {
            ensureActive()
            Timber.e(it, "Failed to import the font")
        }.getOrNull()
    }

    /**
     * The name of the file in [uri], without any directories it may contain.
     */
    private fun getFileName(uri: Uri): String {
        val displayName: String? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        return (displayName ?: uri.lastPathSegment)
            ?.substringAfterLast("/")
            ?.takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: error("Unable to resolve the file name of $uri")
    }

    /**
     * Copies the font file in [sourcePath], which belongs to another widget or a backup, to the
     * staging directory of the widget, keeping its name.
     *
     * @return the path of the copy, or `null` when it fails or [sourcePath] isn't a font file the
     * system can load
     */
    suspend fun stageWidgetFont(directoryName: String, sourcePath: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(sourcePath)
            val stagingDir: File = newFontStagingDir(directoryName = directoryName)
            val fontFile = File(stagingDir, sourceFile.name)

            sourceFile.copyTo(target = fontFile)

            if (TextFontLoader.createTypeface(file = fontFile) == null) {
                Timber.w("Not a valid font file %s", mapOf("sourcePath" to sourcePath))
                stagingDir.deleteRecursively()
                return@runCatching null
            }

            fontFile.path
        }.onFailure { Timber.e(it, "Failed to stage the font") }.getOrNull()
    }

    /**
     * Makes the font in [sourcePath] the only font in the widget directory, replacing a previous
     * file with the same name. A `null` [sourcePath] deletes the font of the widget.
     *
     * The staged file is copied rather than moved, since a widget that is being placed renders
     * from its configuration in memory, which still points at the staged file.
     *
     * @return the path of the saved font, or `null` when [sourcePath] is `null` or it fails
     */
    suspend fun saveWidgetFont(directoryName: String, sourcePath: String?): String? = withContext(Dispatchers.IO) {
        val fontDir: File = getWidgetFontDir(directoryName = directoryName)

        val savedFont: File? = sourcePath?.let(::File)?.let { sourceFile: File ->
            runCatching {
                val fontFile = File(fontDir, sourceFile.name)

                if (sourceFile != fontFile) {
                    fontDir.mkdirs()

                    // Written to a temporary file first, so a placed widget never reads a partial font
                    val tempFile: File = File.createTempFile("font", TMP_SUFFIX, fontDir)
                    sourceFile.copyTo(target = tempFile, overwrite = true)
                    Files.move(tempFile.toPath(), fontFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                fontFile
            }.onFailure { Timber.e(it, "Failed to save the font") }.getOrNull()
        }

        fontDir.listFiles()
            ?.filterNot { it == savedFont }
            ?.forEach { it.delete() }

        savedFont?.path
    }

    /**
     * Deletes the fonts staged for the widget, for when its configuration is discarded.
     */
    suspend fun deleteStagedWidgetFonts(directoryName: String) {
        withContext(Dispatchers.IO) {
            File(fontStagingRootDir, directoryName).deleteRecursively()
        }
    }

    /**
     * Deletes the fonts staged before [timestamp], along with the staging directories of widgets
     * that are left without any.
     */
    suspend fun deleteStagedFontsBefore(timestamp: Long) {
        withContext(Dispatchers.IO) {
            for (widgetStagingDir in fontStagingRootDir.listFiles().orEmpty()) {
                widgetStagingDir.listFiles().orEmpty()
                    .filter { it.lastModified() < timestamp }
                    .forEach { it.deleteRecursively() }

                widgetStagingDir.delete()
            }
        }
    }

    private fun newFontStagingDir(directoryName: String): File {
        return File(fontStagingRootDir, "$directoryName/${UUID.randomUUID()}").apply { mkdirs() }
    }

    /**
     * Copies the photos of the widget in [sourceDirectoryName] to the widget in
     * [targetDirectoryName], see [copyWidgetPhotos].
     */
    suspend fun duplicateWidgetDir(sourceDirectoryName: String, targetDirectoryName: String) {
        withContext(Dispatchers.IO) {
            copyWidgetPhotos(
                sourceDir = getWidgetDir(directoryName = sourceDirectoryName),
                targetDir = getWidgetDir(directoryName = targetDirectoryName),
            )
        }
    }

    /**
     * Copies the photos of the widget in [sourceDir] (a widget directory or an extracted backup)
     * to the widget directory [targetDir]. The font is left out, since the font of a widget only
     * changes when the widget is saved, see [stageWidgetFont].
     *
     * Both widgets can be in use while this runs, with their photos being restored, imported or
     * deleted. Each file is copied to an in-progress file and renamed into place, the same way
     * photos are imported, so the target never has a partial photo. The cropped photos are copied
     * before their originals, since without its original a photo is not listed or restored. A
     * photo deleted from the source in the meantime is skipped.
     *
     * When the copy fails, the photos it added to [targetDir] are deleted, originals first, so the
     * target is left with the photos it had. Photos that the target already had are kept, even if
     * they were copied over.
     */
    private fun copyWidgetPhotos(sourceDir: File, targetDir: File) {
        val targetOriginalPhotosDir: File = getOriginalPhotosDir(targetDir).apply { mkdirs() }
        val addedFiles: MutableList<File> = mutableListOf()

        try {
            for (croppedPhoto in listPhotoFiles(dir = sourceDir)) {
                copyWidgetFile(
                    source = croppedPhoto,
                    target = File(targetDir, croppedPhoto.name),
                    tempDir = targetDir,
                    addedFiles = addedFiles,
                )
            }

            for (originalPhoto in listPhotoFiles(dir = getOriginalPhotosDir(sourceDir))) {
                copyWidgetFile(
                    source = originalPhoto,
                    target = File(targetOriginalPhotosDir, originalPhoto.name),
                    tempDir = targetDir,
                    addedFiles = addedFiles,
                )
            }
        } catch (throwable: Throwable) {
            Timber.w("Deleting the photos of a failed copy %s", mapOf("targetDir" to targetDir.name))

            addedFiles.forEach(File::delete)

            throw throwable
        }
    }

    /**
     * Copies [source] to an in-progress file in [tempDir], which must be the widget directory of
     * [target], then renames it to [target]. When [target] didn't exist yet, it is added to
     * [addedFiles].
     */
    private fun copyWidgetFile(source: File, target: File, tempDir: File, addedFiles: MutableList<File>) {
        val isNewFile: Boolean = !target.exists()
        val tempFile: File = newInProgressFile(directory = tempDir)

        try {
            if (copyFileIfExists(source = source, target = tempFile)) {
                check(tempFile.renameTo(target)) { "Failed to rename ${tempFile.name}" }
                if (isNewFile) addedFiles.add(target)
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Copies [source] to [target] through a single stream, so a concurrent rename over [source]
     * doesn't mix two files, as checking its length afterward would.
     *
     * @return false if [source] was deleted before it could be read
     */
    private fun copyFileIfExists(source: File, target: File): Boolean {
        val input: FileInputStream = try {
            FileInputStream(source)
        } catch (e: FileNotFoundException) {
            if (source.exists()) throw e

            Timber.d("File deleted before it could be copied %s", source.name)
            return false
        }

        input.use { FileOutputStream(target).use(input::copyTo) }

        return true
    }

    /**
     * The photos in [dir], without the in-progress files of imports and restores, see
     * [newInProgressFile].
     */
    private fun listPhotoFiles(dir: File): List<File> {
        return dir.listFiles().orEmpty().filter { file -> file.isFile && !isInProgressFile(name = file.name) }
    }

    private suspend fun getWidgetDir(directoryName: String): File = withContext(Dispatchers.IO) {
        File("$rootDir/$directoryName").apply {
            mkdirs()
        }
    }

    private fun getOriginalPhotosDir(widgetDir: File): File = File(widgetDir, ORIGINAL_DIR_NAME)

    private suspend fun getCurrentPhotoDir(directoryName: String): File = withContext(Dispatchers.IO) {
        File("$rootDir/current_photos/$directoryName").apply {
            mkdirs()
        }
    }

    /**
     * Does not create the directory, since widgets may not have a custom font.
     */
    suspend fun getWidgetFontDir(directoryName: String): File = withContext(Dispatchers.IO) {
        File("$rootDir/$directoryName/$FONTS_DIR_NAME")
    }

    /**
     * Copies the photos and the font of the widget in [directoryName] to a directory named after
     * [appWidgetId] in [destinationDir], with the same layout.
     *
     * The widget can be in use while this runs, so the files that are still being written are left
     * out, and the ones deleted in the meantime are skipped.
     */
    suspend fun exportWidgetDir(directoryName: String, appWidgetId: Int, destinationDir: File) {
        withContext(Dispatchers.IO) {
            val widgetDir: File = getWidgetDir(directoryName = directoryName)
            val exportDir = File(destinationDir, "$appWidgetId")
            val exportOriginalPhotosDir: File = getOriginalPhotosDir(exportDir).apply { mkdirs() }

            for (croppedPhoto in listPhotoFiles(dir = widgetDir)) {
                copyFileIfExists(source = croppedPhoto, target = File(exportDir, croppedPhoto.name))
            }

            for (originalPhoto in listPhotoFiles(dir = getOriginalPhotosDir(widgetDir))) {
                copyFileIfExists(source = originalPhoto, target = File(exportOriginalPhotosDir, originalPhoto.name))
            }

            val fonts: List<File> = File(widgetDir, FONTS_DIR_NAME).listFiles().orEmpty()
                // A font being saved is written to a temporary file first, see saveWidgetFont
                .filter { file -> file.isFile && !file.name.endsWith(TMP_SUFFIX) }

            if (fonts.isNotEmpty()) {
                val exportFontDir: File = File(exportDir, FONTS_DIR_NAME).apply { mkdirs() }

                for (font in fonts) {
                    copyFileIfExists(source = font, target = File(exportFontDir, font.name))
                }
            }
        }
    }

    /**
     * Copies the photos in the extracted backup [sourceDir] to the widget in [directoryName], see
     * [copyWidgetPhotos].
     */
    suspend fun importWidgetDir(directoryName: String, sourceDir: File) {
        withContext(Dispatchers.IO) {
            copyWidgetPhotos(sourceDir = sourceDir, targetDir = getWidgetDir(directoryName = directoryName))
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

    companion object {

        // Max-quality lossy: visually indistinguishable from the source yet still encodes faster
        // than lossless PNG (and faster than lossless WebP, which is slower than PNG).
        // The render cache is the only lossy step throughout processing.
        private const val WIDGET_PHOTO_QUALITY: Int = 100

        private const val WEBP_SUFFIX: String = ".webp"

        private const val TMP_SUFFIX: String = ".tmp"

        private const val IN_PROGRESS_PREFIX: String = "writing"

        private const val ORIGINAL_DIR_NAME: String = "original"

        /** The directory, inside the widget directory, that holds its custom font. */
        const val FONTS_DIR_NAME: String = "fonts"
    }
}
