package com.fibelatti.photowidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import androidx.core.content.edit
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoWidgetStorage @Inject constructor(@ApplicationContext context: Context) {

    private val rootDir by lazy {
        File("${context.filesDir}/widgets").apply {
            mkdirs()
        }
    }

    private val sharedPreferences = context.getSharedPreferences(
        SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val contentResolver = context.contentResolver

    suspend fun newWidgetPhoto(
        appWidgetId: Int,
        source: Uri,
    ): LocalPhoto? = withContext(Dispatchers.IO) {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original").apply { mkdirs() }
        val newPhotoName = "${UUID.randomUUID()}.png"

        val originalPhoto = File("$originalPhotosDir/$newPhotoName")
        val croppedPhoto = File("$widgetDir/$newPhotoName")

        val newFiles = listOf(originalPhoto, croppedPhoto)

        val (originalHeight, originalWidth) = contentResolver.openInputStream(source)
            ?.use { inputStream ->
                val options = Options().apply { inJustDecodeBounds = true }

                BitmapFactory.decodeStream(inputStream, null, options)

                options.outHeight to options.outWidth
            }
            ?: return@withContext null // Exit early if the content can't be resolved

        contentResolver.openInputStream(source).use { inputStream ->
            val bitmapOptions = Options().apply {
                if (originalWidth > 1_000 || originalHeight > 1_000) {
                    if (originalWidth > originalHeight) {
                        inTargetDensity = 1_000
                        inDensity = originalWidth
                    } else {
                        inTargetDensity = 1_000
                        inDensity = originalHeight
                    }
                }
            }
            val importedPhoto = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
                ?: return@withContext null // Exit early if the bitmap can't be decoded

            newFiles.map { file ->
                async {
                    FileOutputStream(file).use { fos ->
                        importedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                }
            }.awaitAll()
        }

        return@withContext LocalPhoto(
            name = newPhotoName,
            path = croppedPhoto.path,
        )
    }

    fun getWidgetPhotos(appWidgetId: Int): List<LocalPhoto> {
        return getWidgetDir(appWidgetId = appWidgetId).let { dir ->
            dir.list { _, name -> name != "original" }
                .orEmpty()
                .map { file -> LocalPhoto(name = file, path = "$dir/$file") }
        }
    }

    suspend fun getCropSources(appWidgetId: Int, photoName: String): Pair<File, File> {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original")

        val originalPhoto = File("$originalPhotosDir/$photoName")
        val croppedPhoto = File("$widgetDir/$photoName")

        if (!originalPhoto.exists()) {
            withContext(Dispatchers.IO) {
                FileInputStream(croppedPhoto).use { fileInputStream ->
                    fileInputStream.copyTo(FileOutputStream(originalPhoto))
                }
            }
        }

        return originalPhoto to croppedPhoto
    }

    fun deleteWidgetPhoto(appWidgetId: Int, photoName: String) {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original")

        with(File("$originalPhotosDir/$photoName")) {
            if (exists()) delete()
        }
        with(File("$widgetDir/$photoName")) {
            if (exists()) delete()
        }
    }

    fun saveWidgetInterval(appWidgetId: Int, interval: PhotoWidgetLoopingInterval) {
        sharedPreferences.edit {
            putString("$PREFERENCE_KEY_PREFIX_INTERVAL$appWidgetId", interval.name)
        }
    }

    fun getWidgetInterval(appWidgetId: Int): PhotoWidgetLoopingInterval? {
        val name = sharedPreferences.getString("$PREFERENCE_KEY_PREFIX_INTERVAL$appWidgetId", null)

        return name?.let(PhotoWidgetLoopingInterval::valueOf)
    }

    fun saveWidgetIndex(appWidgetId: Int, index: Int) {
        sharedPreferences.edit {
            putInt("$PREFERENCE_KEY_PREFIX_INDEX$appWidgetId", index)
        }
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getInt("$PREFERENCE_KEY_PREFIX_INDEX$appWidgetId", 0)
    }

    fun saveWidgetAspectRatio(appWidgetId: Int, aspectRatio: PhotoWidgetAspectRatio) {
        sharedPreferences.edit {
            putString("$PREFERENCE_KEY_ASPECT_RATIO$appWidgetId", aspectRatio.name)
        }
    }

    fun getWidgetAspectRatio(appWidgetId: Int): PhotoWidgetAspectRatio {
        val name = sharedPreferences.getString("$PREFERENCE_KEY_ASPECT_RATIO$appWidgetId", null)

        return name?.let(PhotoWidgetAspectRatio::valueOf) ?: PhotoWidgetAspectRatio.SQUARE
    }

    fun saveWidgetShapeId(appWidgetId: Int, shapeId: String) {
        sharedPreferences.edit {
            putString("$PREFERENCE_KEY_PREFIX_SHAPE$appWidgetId", shapeId)
        }
    }

    fun getWidgetShapeId(appWidgetId: Int): String? {
        return sharedPreferences.getString("$PREFERENCE_KEY_PREFIX_SHAPE$appWidgetId", null)
    }

    fun deleteWidgetData(appWidgetId: Int) {
        getWidgetDir(appWidgetId).deleteRecursively()

        sharedPreferences.edit {
            allPreferenceKeyPrefixes.forEach { prefix ->
                remove("$prefix$appWidgetId")
            }
        }
    }

    fun deleteUnusedWidgetData(existingWidgetIds: List<Int>) {
        val existingWidgetsAsDirName = existingWidgetIds.map { "$it" }.toSet()
        val unusedWidgetIds = rootDir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in existingWidgetsAsDirName }
            .map { it.name.toInt() }

        for (id in unusedWidgetIds) {
            deleteWidgetData(appWidgetId = id)
        }
    }

    fun renameTemporaryWidgetDir(appWidgetId: Int) {
        val tempDir = File("$rootDir/0")
        if (tempDir.exists()) {
            tempDir.renameTo(File("$rootDir/$appWidgetId"))
        }
    }

    private fun getWidgetDir(appWidgetId: Int): File {
        return File("$rootDir/$appWidgetId").apply {
            mkdirs()
        }
    }

    private companion object {
        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.PhotoWidget"

        const val PREFERENCE_KEY_PREFIX_INTERVAL = "appwidget_interval_"
        const val PREFERENCE_KEY_PREFIX_INDEX = "appwidget_index_"
        const val PREFERENCE_KEY_ASPECT_RATIO = "appwidget_aspect_ratio_"
        const val PREFERENCE_KEY_PREFIX_SHAPE = "appwidget_shape_"

        val allPreferenceKeyPrefixes: List<String> = listOf(
            PREFERENCE_KEY_PREFIX_INTERVAL,
            PREFERENCE_KEY_PREFIX_INDEX,
            PREFERENCE_KEY_ASPECT_RATIO,
            PREFERENCE_KEY_PREFIX_SHAPE,
        )
    }
}
