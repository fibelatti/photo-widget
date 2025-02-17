package com.fibelatti.photowidget.widget.data

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.WidgetPhotos
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

@Singleton
class PhotoWidgetStorage @Inject constructor(
    private val sharedPreferences: PhotoWidgetSharedPreferences,
    private val internalFileStorage: PhotoWidgetInternalFileStorage,
    private val externalFileStorage: PhotoWidgetExternalFileStorage,
    private val localPhotoDao: LocalPhotoDao,
    private val displayedPhotoDao: DisplayedPhotoDao,
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

    suspend fun getNewDirPhotos(dirUri: Uri): List<LocalPhoto>? {
        if (dirUri.toString().endsWith("DCIM%2FCamera", ignoreCase = true)) {
            return null
        }

        return try {
            // Traverse the directory structure to ensure that all folders contains less than the limit
            externalFileStorage.getPhotos(
                dirUri = setOf(dirUri),
                croppedPhotos = emptyMap(),
                applyValidation = true,
            )
        } catch (_: InvalidDirException) {
            null
        }
    }

    fun getWidgetPhotos(appWidgetId: Int): Flow<WidgetPhotos> = flow {
        Timber.d("Retrieving photos (appWidgetId=$appWidgetId)")

        val excludedPhotos = getExcludedPhotoIds(appWidgetId = appWidgetId)

        val local = getLocalWidgetPhotos(
            appWidgetId = appWidgetId,
            excludedPhotos = excludedPhotos,
        )

        if (local.current.isNotEmpty()) {
            emit(local)
        }

        val source = getSourceWidgetPhotos(
            appWidgetId = appWidgetId,
            excludedPhotos = excludedPhotos,
        )

        if (local.current.isEmpty()) {
            syncWidgetPhotos(
                appWidgetId = appWidgetId,
                currentPhotos = source.current,
                removedPhotos = source.excluded,
            )
        }

        emit(source)
    }

    private suspend fun getSourceWidgetPhotos(
        appWidgetId: Int,
        excludedPhotos: Set<String>,
    ): WidgetPhotos {
        Timber.d("Retrieving photos from source")

        val source = getWidgetSource(appWidgetId = appWidgetId)

        Timber.d("Widget source: $source")

        val croppedPhotos = internalFileStorage.getWidgetPhotos(
            appWidgetId = appWidgetId,
            source = source,
        ).associateBy { it.photoId }

        Timber.d("Cropped photos found: ${croppedPhotos.size}")

        val loadedPhotos = if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotos(dirUri = getWidgetSyncDir(appWidgetId), croppedPhotos = croppedPhotos)
        } else {
            // Check for legacy storage value
            // Migrate found value to the new storage
            // or retrieve it from the new storage if not found
            val widgetOrder = sharedPreferences.getWidgetOrder(appWidgetId = appWidgetId)
                ?.also { saveWidgetOrder(appWidgetId = appWidgetId, order = it.toSet()) }
                ?: orderDao.getWidgetOrder(widgetId = appWidgetId)

            widgetOrder.ifEmpty(croppedPhotos::keys)
                // Excluded photos weren't always saved with the order
                .plus(excludedPhotos).distinct()
                .mapNotNull(croppedPhotos::get)
        }.groupBy {
            val isExcluded = it.photoId in excludedPhotos || it.photoId.substringAfterLast(SEPARATOR) in excludedPhotos
            !isExcluded
        }

        return WidgetPhotos(
            current = loadedPhotos[true].orEmpty(),
            excluded = loadedPhotos[false].orEmpty(),
        ).also {
            Timber.d("Total photos found: ${it.current.size} current, ${it.excluded.size} excluded.")
        }
    }

    private suspend fun getLocalWidgetPhotos(
        appWidgetId: Int,
        excludedPhotos: Set<String>,
    ): WidgetPhotos {
        Timber.d("Retrieving photos from cache")

        val localPhotos = localPhotoDao.getLocalPhotos(widgetId = appWidgetId)
            .map {
                LocalPhoto(
                    photoId = it.photoId,
                    croppedPhotoPath = it.croppedPhotoPath,
                    originalPhotoPath = it.originalPhotoPath,
                    externalUri = it.externalUri?.let(Uri::parse),
                    timestamp = it.timestamp,
                )
            }
            .groupBy {
                val isExcluded = it.photoId in excludedPhotos ||
                    it.photoId.substringAfterLast(SEPARATOR) in excludedPhotos
                !isExcluded
            }

        return WidgetPhotos(
            current = localPhotos[true].orEmpty(),
            excluded = localPhotos[false].orEmpty(),
        ).also {
            Timber.d("Total local photos found: ${it.current.size} current, ${it.excluded.size} excluded.")
        }
    }

    private suspend fun saveWidgetOrder(appWidgetId: Int, order: Set<String>) {
        orderDao.replaceWidgetOrder(
            widgetId = appWidgetId,
            order = order.mapIndexed { index, photoId ->
                PhotoWidgetOrderDto(
                    widgetId = appWidgetId,
                    photoIndex = index,
                    photoId = photoId,
                )
            },
            idsToDelete = orderDao.getWidgetOrder(widgetId = appWidgetId).subtract(order),
        )
    }

    suspend fun syncWidgetPhotos(
        appWidgetId: Int,
        currentPhotos: List<LocalPhoto>? = null,
        removedPhotos: List<LocalPhoto>? = null,
    ) {
        Timber.d("Syncing photos (appWidgetId=$appWidgetId, fromWidget=${currentPhotos != null})")

        val excludedPhotos: Set<String> = removedPhotos?.map { it.photoId }?.toSet()
            ?: getExcludedPhotoIds(appWidgetId = appWidgetId)
        val allPhotos: List<LocalPhoto> = currentPhotos?.plus(removedPhotos.orEmpty())
            ?: getSourceWidgetPhotos(appWidgetId = appWidgetId, excludedPhotos = excludedPhotos).all()

        val newLocalPhotos: List<LocalPhotoDto> = allPhotos.map { localPhoto ->
            LocalPhotoDto(
                widgetId = appWidgetId,
                photoId = localPhoto.photoId,
                croppedPhotoPath = localPhoto.croppedPhotoPath?.replace("widgets/0", "widgets/$appWidgetId"),
                originalPhotoPath = localPhoto.originalPhotoPath?.replace("widgets/0", "widgets/$appWidgetId"),
                externalUri = localPhoto.externalUri?.toString(),
                timestamp = localPhoto.timestamp,
            )
        }
        val newPhotoIds = newLocalPhotos.map { it.photoId }.toSet()

        localPhotoDao.replacePhotos(
            widgetId = appWidgetId,
            photos = newLocalPhotos,
            idsToDelete = localPhotoDao.getLocalPhotoIds(widgetId = appWidgetId).subtract(newPhotoIds),
        )

        saveWidgetOrder(appWidgetId = appWidgetId, order = newPhotoIds)

        displayedPhotoDao.deletePhotosByPhotoIds(
            widgetId = appWidgetId,
            photoIds = displayedPhotoDao.getDisplayedPhotoIds(widgetId = appWidgetId).subtract(newPhotoIds),
        )
    }

    suspend fun getCurrentPhotoId(appWidgetId: Int): String? {
        return displayedPhotoDao.getCurrentPhotoId(widgetId = appWidgetId)
    }

    suspend fun getDisplayedPhotoIds(appWidgetId: Int): List<String> {
        return displayedPhotoDao.getDisplayedPhotoIds(widgetId = appWidgetId)
    }

    suspend fun clearDisplayedPhotos(appWidgetId: Int) {
        displayedPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
    }

    suspend fun saveDisplayedPhoto(appWidgetId: Int, photoId: String) {
        displayedPhotoDao.savePhoto(
            displayedWidgetPhotoDto = DisplayedWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoId,
                timestamp = System.currentTimeMillis(),
            ),
        )
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

    suspend fun markPhotosForDeletion(appWidgetId: Int, photoIds: Collection<String>) {
        val deletionTimestamp = System.currentTimeMillis() + DELETION_THRESHOLD_MILLIS

        val photos = photoIds.map { photoId ->
            PendingDeletionWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoId,
                deletionTimestamp = deletionTimestamp,
            )
        }

        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        pendingDeletionPhotosDao.savePendingDeletionPhotos(photos = photos)
    }

    suspend fun saveExcludedPhotos(appWidgetId: Int, photoIds: Collection<String>) {
        val photos = photoIds.map { photoId ->
            ExcludedWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoId,
            )
        }

        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        excludedPhotosDao.saveExcludedPhotos(photos = photos)
    }

    suspend fun deletePhotos(appWidgetId: Int, photoIds: Iterable<String>) {
        for (photo in photoIds) {
            internalFileStorage.deleteWidgetPhoto(appWidgetId = appWidgetId, photoId = photo)
        }
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

    fun saveWidgetNextCycleTime(appWidgetId: Int, nextCycleTime: Long?) {
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

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetIndex(appWidgetId = appWidgetId)
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

    fun saveWidgetBorder(appWidgetId: Int, border: PhotoWidgetBorder) {
        sharedPreferences.saveWidgetBorder(appWidgetId = appWidgetId, border = border)
    }

    fun getWidgetBorder(appWidgetId: Int): PhotoWidgetBorder {
        return sharedPreferences.getWidgetBorder(appWidgetId = appWidgetId)
    }

    fun saveWidgetOpacity(appWidgetId: Int, opacity: Float) {
        sharedPreferences.saveWidgetOpacity(appWidgetId = appWidgetId, opacity = opacity)
    }

    fun getWidgetOpacity(appWidgetId: Int): Float {
        return sharedPreferences.getWidgetOpacity(appWidgetId = appWidgetId)
    }

    fun saveWidgetBlackAndWhite(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetBlackAndWhite(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetBlackAndWhite(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetBlackAndWhite(appWidgetId = appWidgetId)
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

    fun saveWidgetDeletionTimestamp(appWidgetId: Int, timestamp: Long?) {
        sharedPreferences.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = timestamp)
    }

    fun getWidgetDeletionTimestamp(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetDeletionTimestamp(appWidgetId = appWidgetId)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        Timber.d("Deleting widget data (appWidgetId=$appWidgetId)")
        sharedPreferences.deleteWidgetData(appWidgetId = appWidgetId)
        internalFileStorage.deleteWidgetData(appWidgetId = appWidgetId)

        localPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        displayedPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        orderDao.deletePhotosByWidgetId(widgetId = appWidgetId)
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
            val deletionTimestamp = getWidgetDeletionTimestamp(appWidgetId = id)
            if (deletionTimestamp == -1L) {
                Timber.d("Widget was kept by the user (appWidgetId=$id)")
                continue
            }

            val deletionInterval = currentTimestamp - deletionTimestamp
            if (deletionInterval <= DELETION_THRESHOLD_MILLIS) {
                Timber.d("Deletion threshold not reached (appWidgetId=$id, interval=$deletionInterval)")
                continue
            }
            deleteWidgetData(appWidgetId = id)
        }

        val photosToDelete = pendingDeletionPhotosDao.getPhotosToDelete(timestamp = currentTimestamp)
        for (photo in photosToDelete) {
            internalFileStorage.deleteWidgetPhoto(appWidgetId = photo.widgetId, photoId = photo.photoId)
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

    companion object {

        private val DELETION_THRESHOLD_MILLIS: Long = 7.days.inWholeMilliseconds

        const val SEPARATOR = "#mpw#"
    }
}
