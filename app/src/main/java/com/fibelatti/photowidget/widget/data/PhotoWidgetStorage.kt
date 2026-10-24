package com.fibelatti.photowidget.widget.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.GifFrames
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.SyncDir
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.model.WidgetOffset
import com.fibelatti.photowidget.model.WidgetPhotos
import com.fibelatti.photowidget.model.allWidgetPhotos
import com.fibelatti.photowidget.model.resolveCurrentPhoto
import com.fibelatti.photowidget.platform.UriPermissionGrantor
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class PhotoWidgetStorage @Inject constructor(
    private val sharedPreferences: PhotoWidgetSharedPreferences,
    private val database: PhotoWidgetDatabase,
    private val internalFileStorage: PhotoWidgetInternalFileStorage,
    private val externalFileStorage: PhotoWidgetExternalFileStorage,
    private val localPhotoDao: LocalPhotoDao,
    private val displayedPhotoDao: DisplayedPhotoDao,
    private val orderDao: PhotoWidgetOrderDao,
    private val pendingDeletionPhotosDao: PendingDeletionWidgetPhotoDao,
    private val excludedPhotosDao: ExcludedWidgetPhotoDao,
    private val widgetDirectoryDao: WidgetDirectoryDao,
    private val advancedScheduleTimeDao: AdvancedScheduleTimeDao,
    private val uriPermissionGrantor: UriPermissionGrantor,
) {

    private val migrationMutex: Mutex = Mutex()
    private var migrationChecked: Boolean = false

    private suspend fun ensureDirectoryMigration() {
        // Fast path
        if (migrationChecked) return

        migrationMutex.withLock {
            // Double-checked locking so concurrent callers can exit early
            if (migrationChecked) return

            if (widgetDirectoryDao.getAllWidgetIds().first().isNotEmpty()) {
                migrationChecked = true
                return
            }

            val knownIds: List<Int> = sharedPreferences.getKnownWidgetIds()
            if (knownIds.isEmpty()) {
                migrationChecked = true
                return
            }

            Timber.i("Populating widget directory mappings for ${knownIds.size} existing widgets")
            widgetDirectoryDao.insert(
                directories = knownIds.map { widgetId: Int ->
                    WidgetDirectoryDto(directoryName = widgetId.toString(), widgetId = widgetId)
                },
            )

            migrationChecked = true
        }
    }

    /**
     * Ensures the widget has a directory row so it is tracked by [getKnownWidgetIds]. Photo widgets
     * get one when their first photo is imported; transparent widgets have no photos, so this is
     * called explicitly when they are saved.
     */
    suspend fun ensureWidgetDirectory(appWidgetId: Int) {
        getOrCreateDirectoryName(appWidgetId = appWidgetId)
    }

    private suspend fun getOrCreateDirectoryName(appWidgetId: Int): String {
        ensureDirectoryMigration()

        val directoryName: String = widgetDirectoryDao.getDirectoryName(appWidgetId)
            ?: createDirectoryName(appWidgetId)

        Timber.d("Directory name: $directoryName %s", mapOf("appWidgetId" to appWidgetId))

        return directoryName
    }

    private suspend fun createDirectoryName(appWidgetId: Int): String {
        val directoryName: String = UUID.randomUUID().toString()
        widgetDirectoryDao.insert(WidgetDirectoryDto(directoryName = directoryName, widgetId = appWidgetId))

        Timber.d("New directory: $directoryName %s", mapOf("appWidgetId" to appWidgetId))

        return directoryName
    }

    suspend fun saveWidgetSource(appWidgetId: Int, source: PhotoWidgetSource) {
        val currentSource: PhotoWidgetSource = sharedPreferences.getWidgetSource(appWidgetId = appWidgetId)
        if (source != currentSource) {
            internalFileStorage.deleteWidgetData(directoryName = getOrCreateDirectoryName(appWidgetId = appWidgetId))

            deleteWidgetTableData(appWidgetId = appWidgetId)

            sharedPreferences.saveWidgetSyncedDir(appWidgetId = appWidgetId, syncDirs = emptySet())
        }

        sharedPreferences.saveWidgetSource(appWidgetId = appWidgetId, source = source)
    }

    fun getWidgetSource(appWidgetId: Int): PhotoWidgetSource {
        return sharedPreferences.getWidgetSource(appWidgetId = appWidgetId)
    }

    fun saveWidgetTransparent(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetTransparent(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetTransparent(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetTransparent(appWidgetId = appWidgetId)
    }

    suspend fun hasActiveGifWidgets(): Boolean {
        return getKnownWidgetIds().first().any { id ->
            getWidgetSource(appWidgetId = id) == PhotoWidgetSource.GIF
        }
    }

    fun saveWidgetSyncedDir(appWidgetId: Int, syncDirs: Set<SyncDir>) {
        externalFileStorage.takePersistableUriPermission(syncDirs = syncDirs)
        sharedPreferences.saveWidgetSyncedDir(appWidgetId = appWidgetId, syncDirs = syncDirs)
    }

    fun removeSyncedDir(appWidgetId: Int, dirUri: Uri) {
        val currentDir = getWidgetSyncDir(appWidgetId = appWidgetId)
        saveWidgetSyncedDir(
            appWidgetId = appWidgetId,
            syncDirs = currentDir.filterNot { it.dir == dirUri }.toSet(),
        )
    }

    fun getWidgetSyncDir(appWidgetId: Int): Set<SyncDir> {
        return sharedPreferences.getWidgetSyncDir(appWidgetId = appWidgetId)
    }

    suspend fun newWidgetPhoto(appWidgetId: Int, source: Uri): LocalPhoto? {
        val directoryName = getOrCreateDirectoryName(appWidgetId)
        return internalFileStorage.newWidgetPhoto(directoryName = directoryName, source = source)
    }

    suspend fun newWidgetPhotosFromGif(appWidgetId: Int, source: Uri): GifFrames {
        val directoryName = getOrCreateDirectoryName(appWidgetId)
        return internalFileStorage.newWidgetPhotosFromGif(directoryName = directoryName, source = source)
    }

    suspend fun getNewDirPhotos(syncDir: SyncDir, sorting: DirectorySorting): List<LocalPhoto>? {
        if (syncDir.dir.toString().endsWith("DCIM%2FCamera", ignoreCase = true)) {
            return null
        }

        return try {
            // Traverse the directory structure to ensure that all folders contains less than the limit
            externalFileStorage.getPhotos(
                syncDirs = setOf(syncDir),
                croppedPhotos = emptyMap(),
                sorting = sorting,
                applyValidation = true,
            )
        } catch (_: InvalidDirException) {
            null
        }
    }

    /**
     * Loads the photos of [appWidgetId], first from the local photo cache and then from the widget
     * source, which also refreshes the cache when it has no photos to show.
     *
     * Collectors that only need the cached photos can stop after the first emission, in which case
     * the source is only read if the cache has no photos to show.
     */
    fun loadWidgetPhotos(appWidgetId: Int): Flow<WidgetPhotos> = flow {
        Timber.i("Retrieving photos %s", mapOf("appWidgetId" to appWidgetId))

        val excludedPhotos: Set<String> = getExcludedPhotoIds(appWidgetId = appWidgetId)

        val local: WidgetPhotos = getLocalWidgetPhotos(
            appWidgetId = appWidgetId,
            excludedPhotos = excludedPhotos,
        )

        if (local.current.isNotEmpty()) {
            emit(local)
        }

        val source: WidgetPhotos = getSourceWidgetPhotos(
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

    /**
     * Resolves the photo [appWidgetId] is currently displaying, reading only that photo from the
     * local photo cache. Returns null when the cache has no photos to show, without reading the
     * widget source.
     */
    suspend fun getCachedCurrentWidgetPhoto(appWidgetId: Int): LocalPhoto? {
        Timber.i("Retrieving current photo %s", mapOf("appWidgetId" to appWidgetId))

        val source: PhotoWidgetSource = getWidgetSource(appWidgetId = appWidgetId)
        val excludedPhotos: Set<String> = getExcludedPhotoIds(appWidgetId = appWidgetId)
        val currentPhotoId: String? = getCurrentPhotoId(appWidgetId = appWidgetId)
            ?.takeUnless { photoId -> isExcluded(photoId = photoId, excludedPhotos = excludedPhotos) }

        val widgetIndex: Int = getWidgetIndex(appWidgetId = appWidgetId)

        val localPhoto: LocalPhotoDto? = currentPhotoId
            ?.let { photoId -> localPhotoDao.getLocalPhoto(widgetId = appWidgetId, photoId = photoId) }
            ?: run {
                val photoId: String? = getSyncedWidgetPhotoIds(
                    appWidgetId = appWidgetId,
                    excludedPhotos = excludedPhotos,
                )
                    .resolveCurrentPhoto(currentPhotoId = currentPhotoId, currentIndex = widgetIndex) { it }

                photoId?.let { localPhotoDao.getLocalPhoto(widgetId = appWidgetId, photoId = it) }
            }

        return localPhoto?.toLocalPhoto(source = source)
    }

    fun getPersistedUriPermissions(): List<Uri> = externalFileStorage.getPersistedUriPermissions()

    /**
     * Returns whether the source of [appWidgetId] can still be read, without listing its photos.
     *
     * Only directory widgets can lose access to their source, which happens when none of the
     * synced directories is in [persistedUris], as returned by [getPersistedUriPermissions]. The
     * source of other widgets is internal storage, which the local photo cache already reflects.
     */
    fun hasSourceAccess(appWidgetId: Int, persistedUris: List<Uri>): Boolean {
        if (getWidgetSource(appWidgetId = appWidgetId) != PhotoWidgetSource.DIRECTORY) return true

        return getWidgetSyncDir(appWidgetId = appWidgetId).any { syncDir -> syncDir.dir in persistedUris }
    }

    suspend fun getSyncedWidgetPhotoIds(appWidgetId: Int): List<String> {
        return getSyncedWidgetPhotoIds(
            appWidgetId = appWidgetId,
            excludedPhotos = getExcludedPhotoIds(appWidgetId = appWidgetId),
        )
    }

    private suspend fun getSyncedWidgetPhotoIds(appWidgetId: Int, excludedPhotos: Set<String>): List<String> {
        val photoIds: List<String> = localPhotoDao.getOrderedLocalPhotoIds(widgetId = appWidgetId)

        return withContext(Dispatchers.Default) {
            photoIds.filterNot { id -> isExcluded(photoId = id, excludedPhotos = excludedPhotos) }
        }
    }

    private fun isExcluded(photoId: String, excludedPhotos: Set<String>): Boolean {
        return photoId in excludedPhotos || photoId.substringAfterLast(SEPARATOR) in excludedPhotos
    }

    /**
     * Returns the photo of [appWidgetId] identified by [photoId], reading only that photo from the
     * local photo cache.
     */
    suspend fun getWidgetPhoto(appWidgetId: Int, photoId: String): LocalPhoto? {
        return localPhotoDao.getLocalPhoto(widgetId = appWidgetId, photoId = photoId)
            ?.toLocalPhoto(source = getWidgetSource(appWidgetId = appWidgetId))
    }

    /**
     * Returns the photo that comes before or after [photoId] in the order of [appWidgetId], skipping
     * excluded photos and wrapping around at either end. The order is read in small batches rather
     * than loading every photo ID.
     *
     * Returns [photoId] itself when it is the only photo to show, and null when it isn't part of the
     * widget order, leaving the caller to resolve it from the full photo list.
     */
    suspend fun getAdjacentWidgetPhotoId(appWidgetId: Int, photoId: String, forward: Boolean): String? {
        val excludedPhotos: Set<String> = getExcludedPhotoIds(appWidgetId = appWidgetId)
        if (isExcluded(photoId = photoId, excludedPhotos = excludedPhotos)) return null

        val photoIndex: Int = localPhotoDao.getLocalPhotoIndex(widgetId = appWidgetId, photoId = photoId)
            ?: return null

        return findWidgetPhotoId(
            appWidgetId = appWidgetId,
            startIndex = photoIndex,
            forward = forward,
            excludedPhotos = excludedPhotos,
        ) ?: findWidgetPhotoId(
            appWidgetId = appWidgetId,
            // Wrapping around always finds a photo: at worst, [photoId] itself
            startIndex = if (forward) Int.MIN_VALUE else Int.MAX_VALUE,
            forward = forward,
            excludedPhotos = excludedPhotos,
        )
    }

    private suspend fun findWidgetPhotoId(
        appWidgetId: Int,
        startIndex: Int,
        forward: Boolean,
        excludedPhotos: Set<String>,
    ): String? {
        var photoIndex: Int = startIndex

        while (true) {
            val batch: List<PhotoWidgetOrderDto> = if (forward) {
                localPhotoDao.getLocalPhotoOrderAfter(
                    widgetId = appWidgetId,
                    photoIndex = photoIndex,
                    limit = ORDER_BATCH_SIZE,
                )
            } else {
                localPhotoDao.getLocalPhotoOrderBefore(
                    widgetId = appWidgetId,
                    photoIndex = photoIndex,
                    limit = ORDER_BATCH_SIZE,
                )
            }

            batch.firstOrNull { !isExcluded(photoId = it.photoId, excludedPhotos = excludedPhotos) }
                ?.let { return it.photoId }

            if (batch.size < ORDER_BATCH_SIZE) return null

            photoIndex = batch.last().photoIndex
        }
    }

    suspend fun getWidgetPhotoCountEstimate(appWidgetId: Int): Int {
        return when (getWidgetSource(appWidgetId = appWidgetId)) {
            PhotoWidgetSource.PHOTOS -> localPhotoDao.countLocalPhotosNotPendingDeletion(widgetId = appWidgetId)
            PhotoWidgetSource.DIRECTORY -> localPhotoDao.countLocalPhotosNotExcluded(widgetId = appWidgetId)
            PhotoWidgetSource.GIF -> localPhotoDao.countLocalPhotos(widgetId = appWidgetId)
        }
    }

    private suspend fun getSourceWidgetPhotos(
        appWidgetId: Int,
        excludedPhotos: Set<String>,
    ): WidgetPhotos {
        Timber.d("Retrieving photos from source")

        val source: PhotoWidgetSource = getWidgetSource(appWidgetId = appWidgetId)

        Timber.d("Widget source: $source")

        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        val croppedPhotos: Map<String, LocalPhoto> = internalFileStorage.getWidgetPhotos(
            directoryName = directoryName,
            source = source,
        ).associateBy { it.photoId }

        Timber.d("Cropped photos found: ${croppedPhotos.size}")

        val loadedPhotos: Map<Boolean, List<LocalPhoto>> = if (source == PhotoWidgetSource.DIRECTORY) {
            val photos: List<LocalPhoto> = externalFileStorage.getPhotos(
                syncDirs = getWidgetSyncDir(appWidgetId),
                croppedPhotos = croppedPhotos,
                sorting = getWidgetSorting(appWidgetId),
            )

            withContext(Dispatchers.Default) {
                photos.groupBy { !isExcluded(photoId = it.photoId, excludedPhotos = excludedPhotos) }
            }
        } else {
            // Check for legacy storage value
            // Migrate found value to the new storage
            // or retrieve it from the new storage if not found
            val widgetOrder: List<String> = sharedPreferences.getWidgetOrder(appWidgetId = appWidgetId)
                ?.also { saveWidgetOrder(appWidgetId = appWidgetId, order = it.toSet()) }
                ?: orderDao.getWidgetOrder(widgetId = appWidgetId)

            withContext(Dispatchers.Default) {
                widgetOrder.ifEmpty(croppedPhotos::keys)
                    // Excluded photos weren't always saved with the order
                    .plus(excludedPhotos).distinct()
                    .mapNotNull(croppedPhotos::get)
                    .groupBy { !isExcluded(photoId = it.photoId, excludedPhotos = excludedPhotos) }
            }
        }

        return WidgetPhotos(
            current = loadedPhotos[true].orEmpty(),
            excluded = loadedPhotos[false].orEmpty(),
            fromCache = false,
        ).also {
            Timber.i("Total photos found: ${it.current.size} current, ${it.excluded.size} excluded.")
        }
    }

    private suspend fun getLocalWidgetPhotos(
        appWidgetId: Int,
        excludedPhotos: Set<String>,
    ): WidgetPhotos {
        Timber.d("Retrieving photos from cache")

        val source: PhotoWidgetSource = getWidgetSource(appWidgetId)

        val localPhotoDtos: List<LocalPhotoDto> = localPhotoDao.getLocalPhotos(widgetId = appWidgetId)

        val localPhotos: Map<Boolean, List<LocalPhoto>> = withContext(Dispatchers.Default) {
            localPhotoDtos
                .map { it.toLocalPhoto(source = source) }
                .groupBy { !isExcluded(photoId = it.photoId, excludedPhotos = excludedPhotos) }
        }

        return WidgetPhotos(
            current = localPhotos[true].orEmpty(),
            excluded = localPhotos[false].orEmpty(),
            fromCache = true,
        ).also {
            Timber.i("Total local photos found: ${it.current.size} current, ${it.excluded.size} excluded.")
        }
    }

    private suspend fun LocalPhotoDto.toLocalPhoto(source: PhotoWidgetSource): LocalPhoto {
        return LocalPhoto(
            photoId = photoId,
            croppedPhotoPath = croppedPhotoPath,
            originalPhotoPath = originalPhotoPath,
            externalUri = externalUri?.toUri(),
            launcherUri = if (source == PhotoWidgetSource.GIF && croppedPhotoPath != null) {
                uriPermissionGrantor(croppedPhotoPath)
            } else {
                null
            },
            timestamp = timestamp,
        )
    }

    private suspend fun saveWidgetOrder(appWidgetId: Int, order: Set<String>) {
        val currentOrder: List<String> = orderDao.getWidgetOrder(widgetId = appWidgetId)

        val (newOrder: List<PhotoWidgetOrderDto>, idsToDelete: Set<String>) = withContext(Dispatchers.Default) {
            val photoWidgetOrder: List<PhotoWidgetOrderDto> = order.mapIndexed { index, photoId ->
                PhotoWidgetOrderDto(
                    widgetId = appWidgetId,
                    photoIndex = index,
                    photoId = photoId,
                )
            }

            photoWidgetOrder to currentOrder.subtract(order)
        }

        orderDao.replaceWidgetOrder(
            widgetId = appWidgetId,
            order = newOrder,
            idsToDelete = idsToDelete,
        )
    }

    suspend fun copyWidgetOrder(sourceWidgetId: Int, targetWidgetId: Int) {
        orderDao.copyWidgetOrder(sourceWidgetId = sourceWidgetId, targetWidgetId = targetWidgetId)
    }

    /**
     * Refreshes the local photo cache of [appWidgetId] from its source.
     *
     * Returns whether the cache actually changed (photos added, removed, or when an existing photo
     * whose file was modified in place).
     */
    suspend fun syncWidgetPhotos(
        appWidgetId: Int,
        currentPhotos: List<LocalPhoto>? = null,
        removedPhotos: List<LocalPhoto>? = null,
    ): Boolean {
        Timber.i("Syncing photos %s", mapOf("appWidgetId" to appWidgetId, "fromWidget" to (currentPhotos != null)))

        val excludedPhotos: Set<String> = removedPhotos?.map { it.photoId }?.toSet()
            ?: getExcludedPhotoIds(appWidgetId = appWidgetId)
        val allPhotos: List<LocalPhoto> = currentPhotos?.plus(removedPhotos.orEmpty())
            ?: getSourceWidgetPhotos(appWidgetId = appWidgetId, excludedPhotos = excludedPhotos).allWidgetPhotos()

        val (newLocalPhotos: List<LocalPhotoDto>, newPhotoIds: Set<String>) = withContext(Dispatchers.Default) {
            val localPhotoDtos: List<LocalPhotoDto> = allPhotos.map { localPhoto: LocalPhoto ->
                LocalPhotoDto(
                    widgetId = appWidgetId,
                    photoId = localPhoto.photoId,
                    croppedPhotoPath = localPhoto.croppedPhotoPath,
                    originalPhotoPath = localPhoto.originalPhotoPath,
                    externalUri = localPhoto.externalUri?.toString(),
                    timestamp = localPhoto.timestamp,
                )
            }

            localPhotoDtos to localPhotoDtos.map { it.photoId }.toSet()
        }

        // Cycling reads the photos together with their order, so both must change at once. Writing
        // also completes when the caller is canceled, so the source read that preceded it isn't lost.
        val existingLocalPhotos: List<LocalPhotoDto> = withContext(NonCancellable) {
            database.withTransaction {
                val localPhotoDtos: List<LocalPhotoDto> = localPhotoDao.getLocalPhotos(widgetId = appWidgetId)
                val displayedPhotoIds: List<String> = displayedPhotoDao.getDisplayedPhotoIds(widgetId = appWidgetId)

                localPhotoDao.replacePhotos(
                    widgetId = appWidgetId,
                    photos = newLocalPhotos,
                    idsToDelete = localPhotoDtos.map { it.photoId }.subtract(newPhotoIds),
                )

                saveWidgetOrder(appWidgetId = appWidgetId, order = newPhotoIds)

                displayedPhotoDao.deletePhotosByPhotoIds(
                    widgetId = appWidgetId,
                    photoIds = displayedPhotoIds.subtract(newPhotoIds),
                )

                localPhotoDtos
            }
        }

        return withContext(Dispatchers.Default) {
            existingLocalPhotos.toSet() != newLocalPhotos.toSet()
        }
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

    suspend fun clearMostRecentPhoto(appWidgetId: Int) {
        displayedPhotoDao.deleteMostRecentPhoto(widgetId = appWidgetId)
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

            PhotoWidgetSource.GIF -> emptySet() // GIF source does not support removing photos
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        return internalFileStorage.getCropSources(directoryName = directoryName, localPhoto = localPhoto)
    }

    suspend fun saveCroppedPhoto(sourcePath: String, destinationPath: String): Boolean {
        return internalFileStorage.saveCroppedPhoto(sourcePath = sourcePath, destinationPath = destinationPath)
    }

    suspend fun replacePhotosForDeletion(appWidgetId: Int, photoIds: Collection<String>) {
        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        appendPhotosForDeletion(appWidgetId = appWidgetId, photoIds = photoIds)
    }

    suspend fun appendPhotosForDeletion(appWidgetId: Int, photoIds: Collection<String>) {
        val deletionTimestamp = System.currentTimeMillis() + DELETION_THRESHOLD_MILLIS

        val photos = photoIds.map { photoId ->
            PendingDeletionWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoId,
                deletionTimestamp = deletionTimestamp,
            )
        }

        pendingDeletionPhotosDao.savePendingDeletionPhotos(photos = photos)
    }

    suspend fun replaceExcludedPhotos(appWidgetId: Int, photoIds: Collection<String>) {
        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        appendExcludedPhotos(appWidgetId = appWidgetId, photoIds = photoIds)
    }

    suspend fun appendExcludedPhotos(appWidgetId: Int, photoIds: Collection<String>) {
        val photos = photoIds.map { photoId ->
            ExcludedWidgetPhotoDto(
                widgetId = appWidgetId,
                photoId = photoId,
            )
        }

        excludedPhotosDao.saveExcludedPhotos(photos = photos)
    }

    /**
     * Deletes the cropped photos of [appWidgetId] that are not in [keepPhotoIds].
     *
     * Only meaningful for [PhotoWidgetSource.DIRECTORY] widgets, where the source can stop including
     * a photo — because a synced directory was removed, or because it stopped syncing its subfolders.
     *
     * [keepPhotoIds] must cover the photos the user removed from the widget as well: their ids live
     * in `excluded_widget_photos` and can be restored, so their crops are deliberately kept. Crops
     * saved before ids carried the full path are matched by their file name.
     */
    suspend fun deleteOrphanedPhotos(appWidgetId: Int, keepPhotoIds: Set<String>) {
        val directoryName: String = getOrCreateDirectoryName(appWidgetId)

        // Crops are stored under their photo id, but ids only started carrying the full path later,
        // so a crop file can also be named after the photo's file name alone.
        val keepFileNames: Set<String> = keepPhotoIds + keepPhotoIds.map { it.substringAfterLast(SEPARATOR) }

        val orphanIds: List<String> = internalFileStorage.getWidgetPhotos(
            directoryName = directoryName,
            source = PhotoWidgetSource.DIRECTORY,
        ).map { it.photoId }
            .filterNot { photoId -> photoId in keepFileNames }

        if (orphanIds.isEmpty()) return

        Timber.i(
            "Deleting orphaned photos %s",
            mapOf("appWidgetId" to appWidgetId, "count" to orphanIds.size),
        )

        deletePhotos(appWidgetId = appWidgetId, photoIds = orphanIds)
    }

    suspend fun deletePhotos(appWidgetId: Int, photoIds: Iterable<String>) {
        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        for (photo in photoIds) {
            internalFileStorage.deleteWidgetPhoto(directoryName = directoryName, photoId = photo)
        }
    }

    fun saveWidgetShuffle(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetShuffle(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetShuffle(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetShuffle(appWidgetId = appWidgetId)
    }

    fun saveWidgetSorting(appWidgetId: Int, sorting: DirectorySorting) {
        sharedPreferences.saveWidgetSorting(appWidgetId = appWidgetId, sorting = sorting)
    }

    fun getWidgetSorting(appWidgetId: Int): DirectorySorting {
        return sharedPreferences.getWidgetSorting(appWidgetId = appWidgetId)
    }

    suspend fun saveWidgetCycleMode(appWidgetId: Int, cycleMode: PhotoWidgetCycleMode) {
        sharedPreferences.saveWidgetCycleMode(appWidgetId = appWidgetId, cycleMode = cycleMode)

        if (cycleMode is PhotoWidgetCycleMode.AdvancedSchedule) {
            saveAdvancedScheduleTimes(appWidgetId = appWidgetId, times = cycleMode.schedule)
        } else {
            deleteAdvancedScheduleTimes(appWidgetId = appWidgetId)
        }
    }

    fun getWidgetCycleModeType(appWidgetId: Int): KClass<out PhotoWidgetCycleMode> {
        return sharedPreferences.getWidgetCycleMode(appWidgetId = appWidgetId)::class
    }

    suspend fun getWidgetCycleMode(appWidgetId: Int): PhotoWidgetCycleMode {
        val cycleMode: PhotoWidgetCycleMode = sharedPreferences.getWidgetCycleMode(appWidgetId = appWidgetId)
        return if (cycleMode !is PhotoWidgetCycleMode.AdvancedSchedule) {
            cycleMode
        } else {
            val times: Map<String, Time> = getAdvancedScheduleTimes(appWidgetId = appWidgetId)
            cycleMode.copy(schedule = times.toMap())
        }
    }

    private suspend fun getAdvancedScheduleTimes(appWidgetId: Int): Map<String, Time> {
        return advancedScheduleTimeDao.getTimes(widgetId = appWidgetId)
            .associate { it.photoId to Time.fromString(it.time) }
    }

    private suspend fun saveAdvancedScheduleTimes(appWidgetId: Int, times: Map<String, Time>) {
        advancedScheduleTimeDao.replaceTimes(
            widgetId = appWidgetId,
            times = times.map { (photoId: String, time: Time) ->
                AdvancedScheduleTimeDto(widgetId = appWidgetId, photoId = photoId, time = time.asString())
            },
        )
    }

    private suspend fun deleteAdvancedScheduleTimes(appWidgetId: Int) {
        advancedScheduleTimeDao.deleteByWidgetId(widgetId = appWidgetId)
    }

    suspend fun copyAdvancedScheduleTimes(sourceWidgetId: Int, targetWidgetId: Int) {
        advancedScheduleTimeDao.copyTimes(sourceWidgetId = sourceWidgetId, targetWidgetId = targetWidgetId)
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

    fun saveWidgetLockedInApp(appWidgetId: Int, value: Boolean) {
        sharedPreferences.saveWidgetLockedInApp(appWidgetId = appWidgetId, value = value)
    }

    fun getWidgetLockedInApp(appWidgetId: Int): Boolean {
        return sharedPreferences.getWidgetLockedInApp(appWidgetId = appWidgetId)
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

    fun saveWidgetShapeRotation(appWidgetId: Int, shapeRotation: Int) {
        sharedPreferences.saveWidgetShapeRotation(appWidgetId = appWidgetId, shapeRotation = shapeRotation)
    }

    fun getWidgetShapeRotation(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetShapeRotation(appWidgetId = appWidgetId)
    }

    fun saveWidgetCornerRadius(appWidgetId: Int, cornerRadius: Int) {
        sharedPreferences.saveWidgetCornerRadius(appWidgetId = appWidgetId, cornerRadius = cornerRadius)
    }

    fun getWidgetCornerRadius(appWidgetId: Int): Int {
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

    fun saveWidgetSaturation(appWidgetId: Int, saturation: Float) {
        sharedPreferences.saveWidgetSaturation(appWidgetId = appWidgetId, saturation = saturation)
    }

    fun getWidgetSaturation(appWidgetId: Int): Float {
        return sharedPreferences.getWidgetSaturation(appWidgetId = appWidgetId)
    }

    fun saveWidgetBrightness(appWidgetId: Int, brightness: Float) {
        sharedPreferences.saveWidgetBrightness(appWidgetId = appWidgetId, brightness = brightness)
    }

    fun getWidgetBrightness(appWidgetId: Int): Float {
        return sharedPreferences.getWidgetBrightness(appWidgetId = appWidgetId)
    }

    fun saveWidgetOffset(appWidgetId: Int, horizontalOffset: Int, verticalOffset: Int) {
        sharedPreferences.saveWidgetOffset(
            appWidgetId = appWidgetId,
            horizontalOffset = horizontalOffset,
            verticalOffset = verticalOffset,
        )
    }

    fun getWidgetOffset(appWidgetId: Int): WidgetOffset {
        return sharedPreferences.getWidgetOffset(appWidgetId = appWidgetId)
    }

    fun saveWidgetPadding(appWidgetId: Int, padding: Int) {
        sharedPreferences.saveWidgetPadding(appWidgetId = appWidgetId, padding = padding)
    }

    fun getWidgetPadding(appWidgetId: Int): Int {
        return sharedPreferences.getWidgetPadding(appWidgetId = appWidgetId)
    }

    fun saveWidgetTapAction(appWidgetId: Int, tapAction: PhotoWidgetTapAction, tapActionArea: TapActionArea) {
        sharedPreferences.saveWidgetTapAction(
            appWidgetId = appWidgetId,
            tapAction = tapAction,
            tapActionArea = tapActionArea,
        )
    }

    fun getWidgetTapAction(appWidgetId: Int, tapActionArea: TapActionArea): PhotoWidgetTapAction {
        return sharedPreferences.getWidgetTapAction(
            appWidgetId = appWidgetId,
            tapActionArea = tapActionArea,
        )
    }

    fun saveWidgetText(appWidgetId: Int, text: PhotoWidgetText) {
        sharedPreferences.saveWidgetText(appWidgetId = appWidgetId, text = text)
    }

    fun getWidgetText(appWidgetId: Int): PhotoWidgetText {
        return sharedPreferences.getWidgetText(appWidgetId = appWidgetId)
    }

    fun saveWidgetGifInterval(appWidgetId: Int, interval: Long) {
        sharedPreferences.saveWidgetGifInterval(appWidgetId = appWidgetId, interval = interval)
    }

    fun getWidgetGifInterval(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetGifInterval(appWidgetId = appWidgetId)
    }

    fun saveWidgetDeletionTimestamp(appWidgetId: Int, timestamp: Long?) {
        sharedPreferences.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = timestamp)
    }

    fun getWidgetDeletionTimestamp(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetDeletionTimestamp(appWidgetId = appWidgetId)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        Timber.i("Deleting widget data %s", mapOf("appWidgetId" to appWidgetId))

        val directoryName: String? = widgetDirectoryDao.getDirectoryName(widgetId = appWidgetId)
        if (directoryName != null) {
            internalFileStorage.deleteWidgetData(directoryName = directoryName)
            widgetDirectoryDao.deleteByWidgetId(widgetId = appWidgetId)
        }

        deleteWidgetTableData(appWidgetId = appWidgetId)

        sharedPreferences.deleteWidgetData(appWidgetId = appWidgetId)
    }

    private suspend fun deleteLegacyDraftWidgetData() {
        Timber.d("Deleting draft widget data")

        val draftWidgetId: Int = LEGACY_DRAFT_WIDGET_ID

        internalFileStorage.deleteWidgetData(directoryName = "$LEGACY_DRAFT_WIDGET_ID")
        widgetDirectoryDao.deleteByWidgetId(widgetId = draftWidgetId)

        deleteWidgetTableData(appWidgetId = draftWidgetId)

        sharedPreferences.deleteWidgetData(appWidgetId = draftWidgetId)
    }

    private suspend fun deleteWidgetTableData(appWidgetId: Int) {
        localPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        displayedPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        orderDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        advancedScheduleTimeDao.deleteByWidgetId(widgetId = appWidgetId)
    }

    /**
     * 1. Receive all IDs known by the OS ([existingWidgetIds]).
     * 2. Get all IDs known by the app ([getKnownWidgetIds]).
     * 3. Identify which IDs are known by the app, but not by the OS. These will be processed by
     * this function.
     * 4. Delete legacy draft data (widgetId = 0, one-time migration).
     * 5. For each identified ID, check if the widget was kept by the user and skip.
     * 6. For each identified ID that was not kept, check if the deletion threshold was not
     * reached yet and skip.
     * 7. Delete any widget data that doesn't match the previous rules.
     * 8. Delete recently removed photos that are past the threshold.
     */
    suspend fun deleteUnusedWidgetData(existingWidgetIds: List<Int>) {
        val unplacedWidgetIds: List<Int> = getKnownWidgetIds().first() - existingWidgetIds.toSet()
        val currentTimestamp: Long = System.currentTimeMillis()

        deleteLegacyDraftWidgetData()

        for (id in unplacedWidgetIds) {
            Timber.d("Stale data found %s", mapOf("appWidgetId" to id))
            val deletionTimestamp: Long = getWidgetDeletionTimestamp(appWidgetId = id)
            if (deletionTimestamp == -1L) {
                Timber.d("Widget was kept by the user %s", mapOf("appWidgetId" to id))
                continue
            }

            val deletionInterval: Long = currentTimestamp - deletionTimestamp
            if (deletionInterval <= DELETION_THRESHOLD_MILLIS) {
                Timber.d(
                    "Deletion threshold not reached %s",
                    mapOf("appWidgetId" to id, "interval" to deletionInterval),
                )
                continue
            }

            deleteWidgetData(appWidgetId = id)
        }

        val photosToDelete = pendingDeletionPhotosDao.getPhotosToDelete(timestamp = currentTimestamp)
        for ((widgetId: Int, photoId: String) in photosToDelete) {
            val directoryName: String? = widgetDirectoryDao.getDirectoryName(widgetId)
            if (directoryName != null) {
                internalFileStorage.deleteWidgetPhoto(directoryName = directoryName, photoId = photoId)
            }
        }
        pendingDeletionPhotosDao.deletePhotosBeforeTimestamp(timestamp = currentTimestamp)
    }

    /**
     * Releases persisted URI permissions that are no longer referenced by any widget.
     *
     * Permissions are taken eagerly when a sync directory, [PhotoWidgetTapAction.FileShortcut] or
     * [PhotoWidgetTapAction.FolderShortcut] is configured, but they cannot be released eagerly: the
     * same URI may be shared across multiple widgets, across the three tap areas of a single widget,
     * or between a sync directory and a tap action. A grant is only orphaned once **no** surviving
     * widget references it.
     *
     * This performs a mark-and-sweep: it collects every URI still referenced by the known widgets,
     * drafts and OS-placed widgets, then releases any persisted grant absent from that set. It is
     * meant to run right after [deleteUnusedWidgetData] so grace-expired widgets are already gone.
     */
    suspend fun releaseUnusedUriPermissions(existingWidgetIds: List<Int>) {
        val referencedWidgetIds: Set<Int> = buildSet {
            addAll(existingWidgetIds)
            addAll(getKnownWidgetIds().first())
            addAll(getDraftWidgetIds().first())
        }

        val referencedUris: Set<Uri> = buildSet {
            for (id in referencedWidgetIds) {
                addAll(getWidgetSyncDir(appWidgetId = id).map { it.dir })
                for (area in TapActionArea.entries) {
                    when (val action = getWidgetTapAction(appWidgetId = id, tapActionArea = area)) {
                        is PhotoWidgetTapAction.FileShortcut if action.fileUri != null -> {
                            add(action.fileUri.toUri())
                        }

                        is PhotoWidgetTapAction.FolderShortcut if action.folderUri != null -> {
                            add(action.folderUri.toUri())
                        }

                        else -> Unit
                    }
                }
            }
        }

        for (uri in externalFileStorage.getPersistedUriPermissions()) {
            if (uri !in referencedUris) {
                externalFileStorage.releasePersistableUriPermission(uri)
            }
        }
    }

    fun getKnownWidgetIds(): Flow<List<Int>> {
        return widgetDirectoryDao.getAllWidgetIds()
            .map { value: List<Int> -> value.filter { id: Int -> id > 0 } }
            .onStart { ensureDirectoryMigration() }
    }

    suspend fun createNewDraftId(): Int {
        ensureDirectoryMigration()
        val minId: Int = widgetDirectoryDao.getMinWidgetId() ?: 0
        return minOf(minId, 0) - 1
    }

    fun getDraftWidgetIds(): Flow<List<Int>> {
        return widgetDirectoryDao.getDraftWidgetIds()
            .onStart { ensureDirectoryMigration() }
    }

    suspend fun migrateDraftToWidget(draftWidgetId: Int, appWidgetId: Int) {
        Timber.i("Migrating draft to widget %s", mapOf("draftWidgetId" to draftWidgetId, "appWidgetId" to appWidgetId))

        // Cycling reads the photos together with their order, so every table must move at once
        database.withTransaction {
            localPhotoDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
            displayedPhotoDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
            orderDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
            pendingDeletionPhotosDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
            excludedPhotosDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
            advancedScheduleTimeDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)

            // `getKnownWidgetIds` reads from `widget_directories`. The `PhotoWidgetProvider.update`
            // flow only falls back to `loadPhotoWidgetUseCase` (which can write to
            // `local_widget_photos` via `syncWidgetPhotos`) once the new ID is "known". The
            // transaction keeps this row absent until everything else is in place, which forces
            // concurrent updates to keep rendering from the pinning cache.
            widgetDirectoryDao.migrateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        }

        sharedPreferences.migrateWidgetData(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
    }

    suspend fun duplicateWidgetDir(originalAppWidgetId: Int, newAppWidgetId: Int) {
        val sourceDirectoryName: String = getOrCreateDirectoryName(originalAppWidgetId)
        val targetDirectoryName: String = getOrCreateDirectoryName(newAppWidgetId)
        internalFileStorage.duplicateWidgetDir(
            sourceDirectoryName = sourceDirectoryName,
            targetDirectoryName = targetDirectoryName,
        )
    }

    suspend fun exportWidgetDir(appWidgetId: Int, destinationDir: File) {
        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        internalFileStorage.exportWidgetDir(
            directoryName = directoryName,
            appWidgetId = appWidgetId,
            destinationDir = destinationDir,
        )
    }

    suspend fun importWidgetDir(appWidgetId: Int, sourceDir: File) {
        val directoryName: String = createDirectoryName(appWidgetId)
        internalFileStorage.importWidgetDir(directoryName = directoryName, sourceDir = sourceDir)
    }

    companion object {

        private val DELETION_THRESHOLD_MILLIS: Long = 7.days.inWholeMilliseconds

        private const val LEGACY_DRAFT_WIDGET_ID: Int = 0

        private const val ORDER_BATCH_SIZE: Int = 50

        const val SEPARATOR = "#mpw#"
    }
}
