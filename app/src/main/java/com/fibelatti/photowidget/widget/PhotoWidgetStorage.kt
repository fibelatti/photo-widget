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
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.enumValueOfOrNull
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

        runCatching {
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
                    file.createNewFile()

                    async {
                        FileOutputStream(file).use { fos ->
                            importedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                    }
                }.awaitAll()
            }

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

    fun getWidgetPhotos(appWidgetId: Int): List<LocalPhoto> {
        val photos = getWidgetDir(appWidgetId = appWidgetId).let { dir ->
            dir.list { _, name -> name != "original" }
                .orEmpty()
                .map { file ->
                    // If a widget was previously saved it's safe to assume all images were cropped
                    LocalPhoto(
                        name = file,
                        path = "$dir/$file",
                    )
                }
        }

        val order = getWidgetOrder(appWidgetId).ifEmpty { photos.map { it.name } }
        val dict = photos.associateBy { it.name }

        return order.mapNotNull { dict[it] }
    }

    suspend fun getCropSources(appWidgetId: Int, photoName: String): Pair<File, File> {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val originalPhotosDir = File("$widgetDir/original")

        val originalPhoto = File("$originalPhotosDir/$photoName")
        val croppedPhoto = File("$widgetDir/$photoName")

        if (!originalPhoto.exists()) {
            withContext(Dispatchers.IO) {
                originalPhoto.createNewFile()

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

    fun saveWidgetOrder(appWidgetId: Int, order: List<String>) {
        val value = if (order.isEmpty()) "" else order.joinToString(separator = ",")

        sharedPreferences.edit {
            putString("${PreferencePrefix.ORDER}$appWidgetId", value)
        }
    }

    fun getWidgetOrder(appWidgetId: Int): List<String> {
        val value = sharedPreferences.getString("${PreferencePrefix.ORDER}$appWidgetId", null)

        return value?.split(",").orEmpty()
    }

    fun saveWidgetInterval(appWidgetId: Int, interval: PhotoWidgetLoopingInterval) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.INTERVAL}$appWidgetId", interval.name)
        }
    }

    fun getWidgetInterval(appWidgetId: Int): PhotoWidgetLoopingInterval? {
        val name = sharedPreferences.getString("${PreferencePrefix.INTERVAL}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetLoopingInterval>(name)
    }

    fun saveWidgetIndex(appWidgetId: Int, index: Int) {
        sharedPreferences.edit {
            putInt("${PreferencePrefix.INDEX}$appWidgetId", index)
        }
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getInt("${PreferencePrefix.INDEX}$appWidgetId", 0)
    }

    fun saveWidgetAspectRatio(appWidgetId: Int, aspectRatio: PhotoWidgetAspectRatio) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.RATIO}$appWidgetId", aspectRatio.name)
        }
    }

    fun getWidgetAspectRatio(appWidgetId: Int): PhotoWidgetAspectRatio {
        val name = sharedPreferences.getString("${PreferencePrefix.RATIO}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetAspectRatio>(name) ?: PhotoWidgetAspectRatio.SQUARE
    }

    fun saveWidgetShapeId(appWidgetId: Int, shapeId: String) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.SHAPE}$appWidgetId", shapeId)
        }
    }

    fun getWidgetShapeId(appWidgetId: Int): String? {
        return sharedPreferences.getString("${PreferencePrefix.SHAPE}$appWidgetId", null)
    }

    fun saveWidgetCornerRadius(appWidgetId: Int, cornerRadius: Float) {
        sharedPreferences.edit {
            putFloat("${PreferencePrefix.CORNER_RADIUS}$appWidgetId", cornerRadius)
        }
    }

    fun getWidgetCornerRadius(appWidgetId: Int): Float {
        return sharedPreferences.getFloat(
            "${PreferencePrefix.CORNER_RADIUS}$appWidgetId",
            PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
        )
    }

    fun saveWidgetTapAction(appWidgetId: Int, tapAction: PhotoWidgetTapAction) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.TAP_ACTION}$appWidgetId", tapAction.name)
        }
    }

    fun getWidgetTapAction(appWidgetId: Int): PhotoWidgetTapAction {
        val name = sharedPreferences.getString("${PreferencePrefix.TAP_ACTION}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetTapAction>(name) ?: PhotoWidgetTapAction.VIEW_FULL_SCREEN
    }

    fun deleteWidgetData(appWidgetId: Int) {
        getWidgetDir(appWidgetId).deleteRecursively()

        sharedPreferences.edit {
            PreferencePrefix.entries.forEach { prefix ->
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

    private enum class PreferencePrefix(val value: String) {
        ORDER(value = "appwidget_order_"),
        INTERVAL(value = "appwidget_interval_"),
        INDEX(value = "appwidget_index_"),
        RATIO(value = "appwidget_aspect_ratio_"),
        SHAPE(value = "appwidget_shape_"),
        CORNER_RADIUS(value = "appwidget_corner_radius_"),
        TAP_ACTION(value = "appwidget_tap_action_"),
        ;

        override fun toString(): String = value
    }

    private companion object {
        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.PhotoWidget"
    }
}
