package com.fibelatti.photowidget.widget

import android.content.Context
import androidx.core.content.edit
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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

    fun newWidgetPhoto(appWidgetId: Int): File {
        return File("${getWidgetDir(appWidgetId)}/${UUID.randomUUID()}.png")
    }

    fun getWidgetPhotos(appWidgetId: Int): List<String> {
        return getWidgetDir(appWidgetId = appWidgetId).let { dir ->
            dir.list().orEmpty().map { file -> "$dir/$file" }
        }
    }

    fun deleteWidgetPhoto(photoPath: String) {
        with(File(photoPath)) {
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
