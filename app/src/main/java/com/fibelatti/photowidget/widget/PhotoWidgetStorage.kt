package com.fibelatti.photowidget.widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import com.fibelatti.photowidget.model.LegacyPhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.toLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.PhotoDecoder
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoWidgetStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetOrderDao: PhotoWidgetOrderDao,
    private val decoder: PhotoDecoder,
) {

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

    fun saveWidgetSource(appWidgetId: Int, source: PhotoWidgetSource) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.SOURCE}$appWidgetId", source.name)
        }
    }

    fun getWidgetSource(appWidgetId: Int): PhotoWidgetSource? {
        val name = sharedPreferences.getString("${PreferencePrefix.SOURCE}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetSource>(name)
    }

    fun saveWidgetSyncedDir(appWidgetId: Int, dirUri: Uri) {
        contentResolver.takePersistableUriPermission(dirUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sharedPreferences.edit {
            putString("${PreferencePrefix.SYNCED_DIR}$appWidgetId", dirUri.toString())
        }
    }

    fun getWidgetSyncDir(appWidgetId: Int): Uri? {
        val uriString = sharedPreferences.getString("${PreferencePrefix.SYNCED_DIR}$appWidgetId", null)

        return uriString?.let(Uri::parse)
    }

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
            decoder.decode(source = source)?.let { importedPhoto ->
                newFiles.map { file ->
                    file.createNewFile()

                    async {
                        FileOutputStream(file).use { fos ->
                            importedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos)
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

    suspend fun getWidgetPhotoCount(appWidgetId: Int): Int = withContext(Dispatchers.IO) {
        if (PhotoWidgetSource.DIRECTORY == getWidgetSource(appWidgetId = appWidgetId)) {
            withDirectoryPhotosCursor(appWidgetId = appWidgetId) { _, cursor ->
                var count = 0

                while (cursor.moveToNext()) {
                    val mimeType = cursor.getString(1)
                    if (mimeType in ALLOWED_TYPES) count += 1
                }

                count
            }
        } else {
            getWidgetDir(appWidgetId = appWidgetId).list { _, name -> name != "original" }?.size
        } ?: 0
    }

    suspend fun getWidgetPhotos(appWidgetId: Int, index: Int? = null): List<LocalPhoto> = withContext(Dispatchers.IO) {
        Timber.d("Retrieving photos (appWidgetId=$appWidgetId)")

        val croppedPhotos = getWidgetDir(appWidgetId = appWidgetId).let { dir ->
            dir.list { _, name -> name != "original" }
                .orEmpty()
                .map { file -> LocalPhoto(name = file, path = "$dir/$file") }
        }
        val dict: Map<String, LocalPhoto> = croppedPhotos.associateBy { it.name }

        Timber.d("Cropped photos found: ${dict.size}")

        val source = getWidgetSource(appWidgetId = appWidgetId).also {
            Timber.d("Widget source: $it")
        }

        return@withContext if (PhotoWidgetSource.DIRECTORY == source) {
            getDirectoryPhotos(appWidgetId = appWidgetId, croppedPhotos = dict, index = index)
        } else {
            getWidgetOrder(appWidgetId = appWidgetId)
                .ifEmpty { dict.keys }
                .withIndex()
                .let { indexedItems ->
                    if (index == null) {
                        indexedItems.mapNotNull { dict[it.value] }
                    } else {
                        listOfNotNull(indexedItems.elementAtOrNull(index)?.value?.let { dict[it] })
                    }
                }
        }.also { Timber.d("Total photos found: ${it.size}") }
    }

    private suspend fun getDirectoryPhotos(
        appWidgetId: Int,
        croppedPhotos: Map<String, LocalPhoto>,
        index: Int?,
    ): List<LocalPhoto> = withDirectoryPhotosCursor(appWidgetId = appWidgetId) { documentUri, cursor ->
        val toLocalPhoto: Cursor.() -> LocalPhoto? = {
            val documentId = getString(0)
            val mimeType = getString(1)
            val documentName = getString(2)
            val documentLastModified = getLong(3)

            val fileUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, documentId)

            if (documentName != null && mimeType in ALLOWED_TYPES && fileUri != null) {
                LocalPhoto(
                    name = documentName,
                    path = croppedPhotos[documentName]?.path,
                    externalUri = fileUri,
                    timestamp = documentLastModified,
                )
            } else {
                null
            }
        }

        buildList {
            if (index == null) {
                while (cursor.moveToNext()) {
                    cursor.toLocalPhoto()?.let { add(it) }
                }
            } else if (cursor.moveToPosition(index)) {
                cursor.toLocalPhoto()?.let { add(it) }
            }
        }.sortedBy { it.timestamp }
    }.orEmpty()

    private suspend inline fun <T> withDirectoryPhotosCursor(
        appWidgetId: Int,
        crossinline block: (documentUri: Uri, Cursor) -> T,
    ): T? = withContext(Dispatchers.IO) {
        getWidgetSyncDir(appWidgetId = appWidgetId)?.let { uri ->
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                /* treeUri = */ uri,
                /* documentId = */ DocumentsContract.getTreeDocumentId(uri),
            )

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                /* treeUri = */ documentUri,
                /* parentDocumentId = */ DocumentsContract.getDocumentId(documentUri),
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )

            contentResolver.query(
                /* uri = */ childrenUri,
                /* projection = */ projection,
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )?.use { cursor -> block(documentUri, cursor) }
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        val widgetDir = getWidgetDir(appWidgetId = appWidgetId)
        val croppedPhoto = File("$widgetDir/${localPhoto.name}")

        if (localPhoto.externalUri != null) {
            return localPhoto.externalUri to Uri.fromFile(croppedPhoto)
        } else {
            val originalPhotosDir = File("$widgetDir/original")
            val originalPhoto = File("$originalPhotosDir/${localPhoto.name}")

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

    suspend fun saveWidgetOrder(appWidgetId: Int, order: List<String>) {
        photoWidgetOrderDao.replaceWidgetOrder(
            widgetId = appWidgetId,
            order = order.mapIndexed { index, photoId ->
                PhotoWidgetOrderDto(
                    widgetId = appWidgetId,
                    photoIndex = index,
                    photoId = photoId,
                )
            },
        )
    }

    suspend fun getWidgetOrder(appWidgetId: Int): List<String> {
        // Check for legacy storage value
        val value = sharedPreferences.getString("${PreferencePrefix.ORDER}$appWidgetId", null)
            ?.split(",")

        if (value != null) {
            // Migrate found value to the new storage
            saveWidgetOrder(appWidgetId, value)
            sharedPreferences.edit { remove("${PreferencePrefix.ORDER}$appWidgetId") }
        }

        // Return it to the caller, or retrieve it from the new storage if not found
        return value ?: photoWidgetOrderDao.getWidgetOrder(appWidgetId = appWidgetId)
    }

    fun saveWidgetShuffle(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.SHUFFLE}$appWidgetId", value)
        }
    }

    fun getWidgetShuffle(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean("${PreferencePrefix.SHUFFLE}$appWidgetId", false)
    }

    fun saveWidgetInterval(appWidgetId: Int, interval: PhotoWidgetLoopingInterval) {
        sharedPreferences.edit {
            remove("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId")
            putLong("${PreferencePrefix.INTERVAL}$appWidgetId", interval.toMinutes())
        }
    }

    fun getWidgetInterval(appWidgetId: Int): PhotoWidgetLoopingInterval? {
        val legacyName = sharedPreferences.getString("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId", null)
        val legacyValue = enumValueOfOrNull<LegacyPhotoWidgetLoopingInterval>(legacyName)
        val value = sharedPreferences.getLong("${PreferencePrefix.INTERVAL}$appWidgetId", 0)

        return when {
            legacyValue != null -> {
                PhotoWidgetLoopingInterval(
                    repeatInterval = legacyValue.repeatInterval,
                    timeUnit = legacyValue.timeUnit,
                )
            }

            value > 0 -> value.toLoopingInterval()

            else -> null
        }
    }

    fun saveWidgetIntervalEnabled(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId", value)
        }
    }

    fun getWidgetIntervalEnabled(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId", true)
    }

    fun saveWidgetIndex(appWidgetId: Int, index: Int) {
        sharedPreferences.edit {
            putInt("${PreferencePrefix.INDEX}$appWidgetId", index)
        }
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getInt("${PreferencePrefix.INDEX}$appWidgetId", 0)
    }

    fun saveWidgetPastIndices(appWidgetId: Int, pastIndices: Set<Int>) {
        sharedPreferences.edit {
            putStringSet("${PreferencePrefix.PAST_INDICES}$appWidgetId", pastIndices.map { "$it" }.toSet())
        }
    }

    fun getWidgetPastIndices(appWidgetId: Int): Set<Int> {
        return sharedPreferences.getStringSet("${PreferencePrefix.PAST_INDICES}$appWidgetId", null)
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    fun saveWidgetAspectRatio(appWidgetId: Int, aspectRatio: PhotoWidgetAspectRatio) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.RATIO}$appWidgetId", aspectRatio.name)
        }
    }

    fun getWidgetAspectRatio(appWidgetId: Int): PhotoWidgetAspectRatio? {
        val name = sharedPreferences.getString("${PreferencePrefix.RATIO}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetAspectRatio>(name)
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

    fun getWidgetTapAction(appWidgetId: Int): PhotoWidgetTapAction? {
        val name = sharedPreferences.getString("${PreferencePrefix.TAP_ACTION}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetTapAction>(name)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        Timber.d("Deleting data (appWidgetId=$appWidgetId)")
        getWidgetDir(appWidgetId).deleteRecursively()

        sharedPreferences.edit {
            PreferencePrefix.entries.forEach { prefix -> remove("$prefix$appWidgetId") }
        }

        photoWidgetOrderDao.deleteWidgetOrder(appWidgetId = appWidgetId)
    }

    suspend fun deleteUnusedWidgetData(existingWidgetIds: List<Int>) {
        val existingWidgetsAsDirName = existingWidgetIds.map { "$it" }.toSet()
        val unusedWidgetIds = rootDir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in existingWidgetsAsDirName }
            .map { it.name.toInt() }

        Timber.d("Deleting temp widget data")
        deleteWidgetData(appWidgetId = 0)

        for (id in unusedWidgetIds) {
            Timber.d("Unused data found (appWidgetId=$id)")
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
        SOURCE(value = "appwidget_source_"),
        SYNCED_DIR(value = "appwidget_synced_dir_"),

        ORDER(value = "appwidget_order_"),
        SHUFFLE(value = "appwidget_shuffle_"),

        /**
         * Key from when the interval was persisted as [LegacyPhotoWidgetLoopingInterval].
         */
        LEGACY_INTERVAL(value = "appwidget_interval_"),

        /**
         * Key from when the interval was migrated to [PhotoWidgetLoopingInterval].
         */
        INTERVAL(value = "appwidget_interval_minutes_"),
        INTERVAL_ENABLED(value = "appwidget_interval_enabled_"),
        INDEX(value = "appwidget_index_"),
        PAST_INDICES(value = "appwidget_past_indices_"),
        RATIO(value = "appwidget_aspect_ratio_"),
        SHAPE(value = "appwidget_shape_"),
        CORNER_RADIUS(value = "appwidget_corner_radius_"),
        TAP_ACTION(value = "appwidget_tap_action_"),
        ;

        override fun toString(): String = value
    }

    private companion object {
        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.PhotoWidget"

        private val ALLOWED_TYPES = arrayOf("image/jpeg", "image/png")
    }
}
