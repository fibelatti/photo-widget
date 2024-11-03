package com.fibelatti.photowidget.widget.data

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class PhotoWidgetStorage @Inject constructor(
    private val sharedPreferences: PhotoWidgetSharedPreferences,
    private val internalFileStorage: PhotoWidgetInternalFileStorage,
    private val externalFileStorage: PhotoWidgetExternalFileStorage,
    private val orderDao: PhotoWidgetOrderDao,
    private val pendingDeletionPhotosDao: PendingDeletionWidgetPhotoDao,
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
        return if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotoCount(dirUri = getWidgetSyncDir(appWidgetId = appWidgetId))
        } else {
            val pendingDeletionPhotos = getPendingDeletionPhotoIds(appWidgetId = appWidgetId)

            internalFileStorage.getWidgetPhotos(appWidgetId = appWidgetId, source = source)
                .filterNot { it.name in pendingDeletionPhotos }
                .size
        }
    }

    suspend fun getWidgetPhotos(appWidgetId: Int, originalPhotos: Boolean = false): List<LocalPhoto> {
        Timber.d("Retrieving photos (appWidgetId=$appWidgetId)")

        val pendingDeletionPhotos = getPendingDeletionPhotoIds(appWidgetId = appWidgetId)
        val source = getWidgetSource(appWidgetId = appWidgetId)
            .also { Timber.d("Widget source: $it") }

        val croppedPhotos = internalFileStorage.getWidgetPhotos(
            appWidgetId = appWidgetId,
            source = source,
            originalPhotos = originalPhotos,
        ).filterNot { it.name in pendingDeletionPhotos }.associateBy { it.name }

        Timber.d("Cropped photos found: ${croppedPhotos.size}")

        return if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotos(dirUri = getWidgetSyncDir(appWidgetId), croppedPhotos = croppedPhotos)
        } else {
            // Check for legacy storage value
            // Migrate found value to the new storage
            // or retrieve it from the new storage if not found
            val widgetOrder = sharedPreferences.getWidgetOrder(appWidgetId = appWidgetId)
                ?.also { saveWidgetOrder(appWidgetId = appWidgetId, order = it) }
                ?: orderDao.getWidgetOrder(appWidgetId = appWidgetId)

            widgetOrder.ifEmpty(croppedPhotos::keys).mapNotNull(croppedPhotos::get)
        }.also { Timber.d("Total photos found: ${it.size}") }
    }

    suspend fun getPendingDeletionPhotos(appWidgetId: Int): List<LocalPhoto> {
        val source = getWidgetSource(appWidgetId = appWidgetId)
        if (PhotoWidgetSource.DIRECTORY == source) {
            return emptyList()
        }

        val pendingDeletionPhotos = getPendingDeletionPhotoIds(appWidgetId = appWidgetId)

        return internalFileStorage.getWidgetPhotos(appWidgetId = appWidgetId, source = source)
            .filter { it.name in pendingDeletionPhotos }
    }

    private suspend fun getPendingDeletionPhotoIds(appWidgetId: Int): Set<String> {
        return pendingDeletionPhotosDao.getPendingDeletionPhotos(widgetId = appWidgetId)
            .map { it.photoId }
            .toSet()
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        return internalFileStorage.getCropSources(appWidgetId = appWidgetId, localPhoto = localPhoto)
    }

    suspend fun markPhotosForDeletion(appWidgetId: Int, photoNames: Set<String>) {
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
        Timber.d("Deleting data (appWidgetId=$appWidgetId)")
        internalFileStorage.deleteWidgetData(appWidgetId = appWidgetId)
        sharedPreferences.deleteWidgetData(appWidgetId = appWidgetId)
        orderDao.deleteWidgetOrder(appWidgetId = appWidgetId)
    }

    suspend fun deleteUnusedWidgetData(existingWidgetIds: List<Int>) {
        val unusedWidgetIds = internalFileStorage.getWidgetIds() - existingWidgetIds.toSet()
        val pendingDeletionIds = getPendingDeletionWidgetIds()
        val currentTimestamp = System.currentTimeMillis()

        Timber.d("Deleting temp widget data")
        deleteWidgetData(appWidgetId = 0)

        for (id in unusedWidgetIds + pendingDeletionIds) {
            Timber.d("Unused data found (appWidgetId=$id)")

            val deletionInterval = currentTimestamp - getWidgetDeletionTimestamp(appWidgetId = id)
            if (deletionInterval <= DELETION_THRESHOLD_MILLIS) {
                Timber.d("Deletion threshold not reached (appWidgetId=$id)")
                return
            }
            deleteWidgetData(appWidgetId = id)
        }

        val photosToDelete = pendingDeletionPhotosDao.getPhotosToDelete(timestamp = currentTimestamp)
        for (photo in photosToDelete) {
            internalFileStorage.deleteWidgetPhoto(appWidgetId = photo.widgetId, photoName = photo.photoId)
        }
        pendingDeletionPhotosDao.deletePhotosBeforeTimestamp(timestamp = currentTimestamp)
    }

    fun getPendingDeletionWidgetIds(): List<Int> {
        return sharedPreferences.getPendingDeletionWidgetIds()
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

        private const val DELETION_THRESHOLD_MILLIS: Long = 7 * 24 * 60 * 60 * 1_000
    }
}
