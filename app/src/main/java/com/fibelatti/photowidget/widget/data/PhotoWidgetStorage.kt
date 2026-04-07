package com.fibelatti.photowidget.widget.data

import android.net.Uri
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.WidgetOffset
import com.fibelatti.photowidget.model.WidgetPhotos
import com.fibelatti.photowidget.model.allWidgetPhotos
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val widgetDirectoryDao: WidgetDirectoryDao,
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

    private suspend fun getOrCreateDirectoryName(appWidgetId: Int): String {
        ensureDirectoryMigration()

        val directoryName: String = widgetDirectoryDao.getDirectoryName(appWidgetId)
            ?: createDirectoryName(appWidgetId)

        Timber.d("Directory name (appWidgetId=$appWidgetId): $directoryName.")

        return directoryName
    }

    private suspend fun createDirectoryName(appWidgetId: Int): String {
        val directoryName: String = UUID.randomUUID().toString()
        widgetDirectoryDao.insert(WidgetDirectoryDto(directoryName = directoryName, widgetId = appWidgetId))

        Timber.d("New directory (appWidgetId=$appWidgetId): $directoryName")

        return directoryName
    }

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
        val directoryName = getOrCreateDirectoryName(appWidgetId)
        return internalFileStorage.newWidgetPhoto(directoryName = directoryName, source = source)
    }

    suspend fun getNewDirPhotos(dirUri: Uri, sorting: DirectorySorting): List<LocalPhoto>? {
        if (dirUri.toString().endsWith("DCIM%2FCamera", ignoreCase = true)) {
            return null
        }

        return try {
            // Traverse the directory structure to ensure that all folders contains less than the limit
            externalFileStorage.getPhotos(
                dirUri = setOf(dirUri),
                croppedPhotos = emptyMap(),
                sorting = sorting,
                applyValidation = true,
            )
        } catch (_: InvalidDirException) {
            null
        }
    }

    fun loadWidgetPhotos(appWidgetId: Int): Flow<WidgetPhotos> = flow {
        Timber.i("Retrieving photos (appWidgetId=$appWidgetId)")

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

    suspend fun getWidgetPhotoCountEstimate(appWidgetId: Int): Int {
        return localPhotoDao.getLocalPhotos(widgetId = appWidgetId)
            .map { it.photoId }
            .minus(getExcludedPhotoIds(appWidgetId = appWidgetId))
            .size
    }

    private suspend fun getSourceWidgetPhotos(
        appWidgetId: Int,
        excludedPhotos: Set<String>,
    ): WidgetPhotos {
        Timber.d("Retrieving photos from source")

        val source = getWidgetSource(appWidgetId = appWidgetId)

        Timber.d("Widget source: $source")

        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        val croppedPhotos = internalFileStorage.getWidgetPhotos(
            directoryName = directoryName,
            source = source,
        ).associateBy { it.photoId }

        Timber.d("Cropped photos found: ${croppedPhotos.size}")

        val loadedPhotos = if (PhotoWidgetSource.DIRECTORY == source) {
            externalFileStorage.getPhotos(
                dirUri = getWidgetSyncDir(appWidgetId),
                croppedPhotos = croppedPhotos,
                sorting = getWidgetSorting(appWidgetId),
            )
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

    suspend fun copyWidgetOrder(sourceWidgetId: Int, targetWidgetId: Int) {
        orderDao.copyWidgetOrder(sourceWidgetId = sourceWidgetId, targetWidgetId = targetWidgetId)
    }

    suspend fun syncWidgetPhotos(
        appWidgetId: Int,
        currentPhotos: List<LocalPhoto>? = null,
        removedPhotos: List<LocalPhoto>? = null,
    ) {
        Timber.i("Syncing photos (appWidgetId=$appWidgetId, fromWidget=${currentPhotos != null})")

        val excludedPhotos: Set<String> = removedPhotos?.map { it.photoId }?.toSet()
            ?: getExcludedPhotoIds(appWidgetId = appWidgetId)
        val allPhotos: List<LocalPhoto> = currentPhotos?.plus(removedPhotos.orEmpty())
            ?: getSourceWidgetPhotos(appWidgetId = appWidgetId, excludedPhotos = excludedPhotos).allWidgetPhotos()

        val newLocalPhotos: List<LocalPhotoDto> = allPhotos.map { localPhoto: LocalPhoto ->
            LocalPhotoDto(
                widgetId = appWidgetId,
                photoId = localPhoto.photoId,
                croppedPhotoPath = localPhoto.croppedPhotoPath,
                originalPhotoPath = localPhoto.originalPhotoPath,
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
        }
    }

    suspend fun getCropSources(appWidgetId: Int, localPhoto: LocalPhoto): Pair<Uri, Uri> {
        val directoryName: String = getOrCreateDirectoryName(appWidgetId)
        return internalFileStorage.getCropSources(directoryName = directoryName, localPhoto = localPhoto)
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

    fun saveWidgetDeletionTimestamp(appWidgetId: Int, timestamp: Long?) {
        sharedPreferences.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = timestamp)
    }

    fun getWidgetDeletionTimestamp(appWidgetId: Int): Long {
        return sharedPreferences.getWidgetDeletionTimestamp(appWidgetId = appWidgetId)
    }

    suspend fun deleteWidgetData(appWidgetId: Int) {
        Timber.i("Deleting widget data (appWidgetId=$appWidgetId)")

        val directoryName: String? = widgetDirectoryDao.getDirectoryName(widgetId = appWidgetId)
        if (directoryName != null) {
            internalFileStorage.deleteWidgetData(directoryName = directoryName)
            widgetDirectoryDao.deleteByWidgetId(widgetId = appWidgetId)
        }

        localPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        displayedPhotoDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        orderDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)
        excludedPhotosDao.deletePhotosByWidgetId(widgetId = appWidgetId)

        sharedPreferences.deleteWidgetData(appWidgetId = appWidgetId)
    }

    private suspend fun deleteLegacyDraftWidgetData() {
        Timber.d("Deleting draft widget data")

        val draftWidgetId: Int = LEGACY_DRAFT_WIDGET_ID

        internalFileStorage.deleteWidgetData(directoryName = "$LEGACY_DRAFT_WIDGET_ID")
        widgetDirectoryDao.deleteByWidgetId(widgetId = draftWidgetId)
        localPhotoDao.deletePhotosByWidgetId(widgetId = draftWidgetId)
        displayedPhotoDao.deletePhotosByWidgetId(widgetId = draftWidgetId)
        orderDao.deletePhotosByWidgetId(widgetId = draftWidgetId)
        pendingDeletionPhotosDao.deletePhotosByWidgetId(widgetId = draftWidgetId)
        excludedPhotosDao.deletePhotosByWidgetId(widgetId = draftWidgetId)
        sharedPreferences.deleteWidgetData(appWidgetId = draftWidgetId)
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
            Timber.d("Stale data found (appWidgetId=$id)")
            val deletionTimestamp: Long = getWidgetDeletionTimestamp(appWidgetId = id)
            if (deletionTimestamp == -1L) {
                Timber.d("Widget was kept by the user (appWidgetId=$id)")
                continue
            }

            val deletionInterval: Long = currentTimestamp - deletionTimestamp
            if (deletionInterval <= DELETION_THRESHOLD_MILLIS) {
                Timber.d("Deletion threshold not reached (appWidgetId=$id, interval=$deletionInterval)")
                continue
            }

            deleteWidgetData(appWidgetId = id)
        }

        val photosToDelete = pendingDeletionPhotosDao.getPhotosToDelete(timestamp = currentTimestamp)
        for (photo in photosToDelete) {
            val directoryName: String? = widgetDirectoryDao.getDirectoryName(photo.widgetId)
            if (directoryName != null) {
                internalFileStorage.deleteWidgetPhoto(directoryName = directoryName, photoId = photo.photoId)
            }
        }
        pendingDeletionPhotosDao.deletePhotosBeforeTimestamp(timestamp = currentTimestamp)
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
        Timber.i("Migrating draft to widget (draftWidgetId=$draftWidgetId, appWidgetId=$appWidgetId)")

        widgetDirectoryDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        localPhotoDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        displayedPhotoDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        orderDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        pendingDeletionPhotosDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
        excludedPhotosDao.updateWidgetId(oldWidgetId = draftWidgetId, newWidgetId = appWidgetId)
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

        const val SEPARATOR = "#mpw#"
    }
}
