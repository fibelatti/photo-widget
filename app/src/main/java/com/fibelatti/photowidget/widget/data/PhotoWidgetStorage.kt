package com.fibelatti.photowidget.widget.data

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.WidgetPhotos
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import timber.log.Timber

@Singleton
class PhotoWidgetStorage @Inject constructor(
    private val sharedPreferences: PhotoWidgetSharedPreferences,
    private val internalFileStorage: PhotoWidgetInternalFileStorage,
    private val externalFileStorage: PhotoWidgetExternalFileStorage,
    private val orderDao: PhotoWidgetOrderDao,
    private val pendingDeletionPhotosDao: PendingDeletionWidgetPhotoDao,
    private val excludedPhotosDao: ExcludedWidgetPhotoDao,
) {

    fun saveWidgetSource(appWidgetId: Int, source: PhotoWidgetSource) {
        sharedPreferences.saveWidgetSource(appWidgetId = appWidgetId, source = source)
    }

    fun getWidgetSource(appWidgetId: Int): PhotoWidgetSource {
        return sharedPreferences.getWidgetSource(appWidgetId = appWidgetId)
    }

    fun saveWidgetSyncedDir(appWidgetId: Int, dirUri: Set<Uri>) {
        externalFileStorage.takePersistableUriPermission(dirUri = dirUri)
        sharedPreferences.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = dirUri)
    }

    fun removeSyncedDir(appWidgetId: Int, dirUri: Uri) {
        val currentDir = getWidgetSyncDir(appWidgetId = appWidgetId)
        saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = currentDir - dirUri)
    }

    fun getWidgetSyncDir(appWidgetId: Int): Set<Uri> {
        return sharedPreferences.getWidgetSyncDir(appWidgetId = appWidgetId)
    }

    suspend fun newWidgetPhoto(appWidgetId: Int, source: Uri): LocalPhoto? {
        return internalFileStorage.newWidgetPhoto(appWidgetId = appWidgetId, source = source)
    }

    suspend fun isValidDir(dirUri: Uri): Boolean {
        return externalFileStorage.isValidDir(dirUri = dirUri)
    }

    suspend fun getWidgetPhotoCount(appWidgetId: Int): Int {
        val source = getWidgetSource(appWidgetId = appWidgetId)
        val excludedPhotoIds = getExcludedPhotoIds(appWidgetId = appWidgetId)

        return if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotoCount(
                dirUri = getWidgetSyncDir(appWidgetId = appWidgetId),
                excludedPhotoIds = excludedPhotoIds,
            )
        } else {
            internalFileStorage.getWidgetPhotos(appWidgetId = appWidgetId, source = source)
                .filterNot { it.name in excludedPhotoIds }
                .size
        }
    }

    suspend fun getWidgetPhotos(appWidgetId: Int): WidgetPhotos {
        Timber.d("Retrieving photos (appWidgetId=$appWidgetId)")

        val source = getWidgetSource(appWidgetId = appWidgetId)

        Timber.d("Widget source: $source")

        val croppedPhotos = internalFileStorage.getWidgetPhotos(
            appWidgetId = appWidgetId,
            source = source,
        ).associateBy { it.name }

        Timber.d("Cropped photos found: ${croppedPhotos.size}")

        val excludedPhotos = getExcludedPhotoIds(appWidgetId = appWidgetId)
        val loadedPhotos = if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotos(dirUri = getWidgetSyncDir(appWidgetId), croppedPhotos = croppedPhotos)
        } else {
            // Check for legacy storage value
            // Migrate found value to the new storage
            // or retrieve it from the new storage if not found
            val widgetOrder = sharedPreferences.getWidgetOrder(appWidgetId = appWidgetId)
                ?.also { saveWidgetOrder(appWidgetId = appWidgetId, order = it) }
                ?: orderDao.getWidgetOrder(appWidgetId = appWidgetId)

            widgetOrder.ifEmpty(croppedPhotos::keys).mapNotNull(croppedPhotos::get)
        }.groupBy { it.name !in excludedPhotos }

        return WidgetPhotos(
            current = loadedPhotos[true].orEmpty(),
            excluded = loadedPhotos[false].orEmpty(),
        ).also {
            Timber.d("Total photos found: ${it.current.size} current, ${it.excluded.size} excluded.")
        }
    }

    suspend fun getExcludedPhotoIds(appWidgetId: Int): Set<String> {
        return when (getWidgetSource(appWidgetId = appWidgetId)) {
            PhotoWidgetSource.PHOTOS -> {
                pendingDeletionPhotosDao.getPendingDeletionPhotos(widgetId = appWidgetId)
                    .map { it.photoId }
                    .toSet()
            }

            PhotoWidgetSource.DIRECTORY -> {
                excludedPhotosDao.getExcludedPhotos(widgetId = appWidgetId)
                    .map { it.photoId }
                    .toSet()
            }
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        return internalFileStorage.getCropSources(appWidgetId = appWidgetId, localPhoto = localPhoto)
    }

    suspend fun markPhotosForDeletion(appWidgetId: Int, photoNames: Collection<String>) {
        val deletionTimestamp = System.currentTimeMillis() + DELETION_THRESHOLD_MILLIS

        val photos = photoNames.map { photoName ->
            PendingDeletionWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoName,
                deletionTimestamp = deletionTimestamp,
            )
        }

        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        pendingDeletionPhotosDao.savePendingDeletionPhotos(photos = photos)
    }

    suspend fun saveExcludedPhotos(appWidgetId: Int, photoNames: Collection<String>) {
        val photos = photoNames.map { photoName ->
            ExcludedWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoName,
            )
        }

        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        excludedPhotosDao.saveExcludedPhotos(photos = photos)
    }

    suspend fun deletePhotos(appWidgetId: Int, photoNames: Iterable<String>) {
        for (photo in photoNames) {
            internalFileStorage.deleteWidgetPhoto(appWidgetId = appWidgetId, photoName = photo)
        }
    }

    suspend fun saveWidgetOrder(appWidgetId: Int, order: List<String>) {
        orderDao.replaceWidgetOrder(
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

    fun saveWidgetShuffle(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetShuffle(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetShuffle(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetShuffle(appWidgetId = appWidgetId)
    }

    fun saveWidgetCycleMode(appWidgetId: Int, cycleMode: PhotoWidgetCycleMode) {
        sharedPreferences.saveWidgetCycleMode(appWidgetId = appWidgetId, cycleMode = cycleMode)
    }

    fun getWidgetCycleMode(appWidgetId: Int): PhotoWidgetCycleMode {
        return sharedPreferences.getWidgetCycleMode(appWidgetId = appWidgetId)
    }

    fun saveWidgetNextCycleTime(appWidgetId: Int, nextCycleTime: Long) {
        sharedPreferences.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = nextCycleTime)
    }

    fun getWidgetNextCycleTime(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetNextCycleTime(appWidgetId = appWidgetId)
    }

    fun saveWidgetCyclePaused(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetCyclePaused(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetCyclePaused(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetCyclePaused(appWidgetId = appWidgetId)
    }

    fun saveWidgetIndex(appWidgetId: Int, index: Int) {
        sharedPreferences.saveWidgetIndex(appWidgetId = appWidgetId, index = index)
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetIndex(appWidgetId = appWidgetId)
    }

    fun saveWidgetPastIndices(appWidgetId: Int, pastIndices: Set<Int>) {
        sharedPreferences.saveWidgetPastIndices(appWidgetId = appWidgetId, pastIndices = pastIndices)
    }

    fun getWidgetPastIndices(appWidgetId: Int): Set<Int> {
        return sharedPreferences.getWidgetPastIndices(appWidgetId = appWidgetId)
    }

    fun saveWidgetAspectRatio(appWidgetId: Int, aspectRatio: PhotoWidgetAspectRatio) {
        sharedPreferences.saveWidgetAspectRatio(appWidgetId = appWidgetId, aspectRatio = aspectRatio)
    }

    fun getWidgetAspectRatio(appWidgetId: Int): PhotoWidgetAspectRatio {
        return sharedPreferences.getWidgetAspectRatio(appWidgetId = appWidgetId)
    }

    fun saveWidgetShapeId(appWidgetId: Int, shapeId: String) {
        sharedPreferences.saveWidgetShapeId(appWidgetId = appWidgetId, shapeId = shapeId)
    }

    fun getWidgetShapeId(appWidgetId: Int): String {
        return sharedPreferences.getWidgetShapeId(appWidgetId = appWidgetId)
    }

    fun saveWidgetCornerRadius(appWidgetId: Int, cornerRadius: Float) {
        sharedPreferences.saveWidgetCornerRadius(appWidgetId = appWidgetId, cornerRadius = cornerRadius)
    }

    fun getWidgetCornerRadius(appWidgetId: Int): Float {
        return sharedPreferences.getWidgetCornerRadius(appWidgetId = appWidgetId)
    }

    fun saveWidgetBorderColor(appWidgetId: Int, colorHex: String?, width: Int) {
        sharedPreferences.saveWidgetBorderColor(appWidgetId = appWidgetId, colorHex = colorHex, width = width)
    }

    fun getWidgetBorderColorHex(appWidgetId: Int): String? {
        return sharedPreferences.getWidgetBorderColorHex(appWidgetId = appWidgetId)
    }

    fun getWidgetBorderWidth(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetBorderWidth(appWidgetId = appWidgetId)
    }

    fun saveWidgetOpacity(appWidgetId: Int, opacity: Float) {
        sharedPreferences.saveWidgetOpacity(appWidgetId = appWidgetId, opacity = opacity)
    }

    fun getWidgetOpacity(appWidgetId: Int): Float {
        return sharedPreferences.getWidgetOpacity(appWidgetId = appWidgetId)
    }

    fun saveWidgetOffset(appWidgetId: Int, horizontalOffset: Int, verticalOffset: Int) {
        sharedPreferences.saveWidgetOffset(
            appWidgetId = appWidgetId,
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
        )
    }

    fun getWidgetOffset(appWidgetId: Int): Pair<Int, Int> {
        return sharedPreferences.getWidgetOffset(appWidgetId = appWidgetId)
    }

    fun saveWidgetPadding(appWidgetId: Int, padding: Int) {
        sharedPreferences.saveWidgetPadding(appWidgetId = appWidgetId, padding = padding)
    }

    fun getWidgetPadding(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetPadding(appWidgetId = appWidgetId)
    }

    fun saveWidgetTapAction(appWidgetId: Int, tapAction: PhotoWidgetTapAction) {
        sharedPreferences.saveWidgetTapAction(appWidgetId = appWidgetId, tapAction = tapAction)
    }

    fun getWidgetTapAction(appWidgetId: Int): PhotoWidgetTapAction {
        return sharedPreferences.getWidgetTapAction(appWidgetId = appWidgetId)
    }

    fun saveWidgetDeletionTimestamp(appWidgetId: Int, timestamp: Long) {
        sharedPreferences.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = timestamp)
    }

    fun getWidgetDeletionTimestamp(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetDeletionTimestamp(appWidgetId = appWidgetId)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        Timber.d("Deleting widget data (appWidgetId=$appWidgetId)")
        sharedPreferences.deleteWidgetData(appWidgetId = appWidgetId)
        internalFileStorage.deleteWidgetData(appWidgetId = appWidgetId)
        orderDao.deleteWidgetOrder(appWidgetId = appWidgetId)
        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
    }

    suspend fun deleteUnusedWidgetData(existingWidgetIds: List<Int>) {
        val unusedWidgetIds = getKnownWidgetIds().filterNot { it == 0 || it in existingWidgetIds }
        val currentTimestamp = System.currentTimeMillis()

        Timber.d("Deleting draft widget data")
        deleteWidgetData(appWidgetId = 0)

        for (id in unusedWidgetIds) {
            Timber.d("Stale data found (appWidgetId=$id)")

            val deletionInterval = currentTimestamp - getWidgetDeletionTimestamp(appWidgetId = id)
            if (deletionInterval <= DELETION_THRESHOLD_MILLIS) {
                Timber.d("Deletion threshold not reached (appWidgetId=$id, interval=$deletionInterval)")
                continue
            }
            deleteWidgetData(appWidgetId = id)
        }

        val photosToDelete = pendingDeletionPhotosDao.getPhotosToDelete(timestamp = currentTimestamp)
        for (photo in photosToDelete) {
            internalFileStorage.deleteWidgetPhoto(appWidgetId = photo.widgetId, photoName = photo.photoId)
        }
        pendingDeletionPhotosDao.deletePhotosBeforeTimestamp(timestamp = currentTimestamp)
    }

    fun getKnownWidgetIds(): List<Int> {
        return sharedPreferences.getKnownWidgetIds()
    }

    suspend fun renameTemporaryWidgetDir(appWidgetId: Int) {
        internalFileStorage.renameTemporaryWidgetDir(appWidgetId = appWidgetId)
    }

    suspend fun duplicateWidgetDir(originalAppWidgetId: Int, newAppWidgetId: Int) {
        internalFileStorage.duplicateWidgetDir(
            originalAppWidgetId = originalAppWidgetId,
            newAppWidgetId = newAppWidgetId,
        )
    }

    private companion object {

        private val DELETION_THRESHOLD_MILLIS: Long = 7.days.inWholeMilliseconds
    }
}
