package com.fibelatti.photowidget.widget

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun newWidgetPhoto(appWidgetId: Int, source: Uri): LocalPhoto {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original").apply { mkdirs() }
        val newPhotoName = "${UUID.randomUUID()}.png"

        val originalPhoto = File("$originalPhotosDir/$newPhotoName")
        val croppedPhoto = File("$widgetDir/$newPhotoName")

        contentResolver.openInputStream(source)?.use { inputStream ->
            inputStream.copyTo(FileOutputStream(originalPhoto))
        }

        FileInputStream(originalPhoto).use { fileInputStream ->
            fileInputStream.copyTo(FileOutputStream(croppedPhoto))
        }

        return LocalPhoto(name = newPhotoName, path = croppedPhoto.path)
    }

    fun getWidgetPhotos(appWidgetId: Int): List<LocalPhoto> {
        return getWidgetDir(appWidgetId = appWidgetId).let { dir ->
            dir.list { _, name -> name != "original" }
                .orEmpty()
                .map { file -> LocalPhoto(name = file, path = "$dir/$file") }
        }
    }

    fun getCropSources(appWidgetId: Int, photoName: String): Pair<File, File> {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original")

        val originalPhoto = File("$originalPhotosDir/$photoName")
        val croppedPhoto = File("$widgetDir/$photoName")

        if (!originalPhoto.exists()) {
            FileInputStream(croppedPhoto).use { fileInputStream ->
                fileInputStream.copyTo(FileOutputStream(originalPhoto))
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
