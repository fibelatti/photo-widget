package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapActions
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.coerceTapActions
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.DuplicatePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.RestoreWidgetUseCase
import com.fibelatti.photowidget.widget.SanitizeTapActionsUseCase
import com.fibelatti.photowidget.widget.SavePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class PhotoWidgetConfigureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoWidgetStorage: PhotoWidgetStorage,
    loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val sanitizeTapActionsUseCase: SanitizeTapActionsUseCase,
    private val duplicatePhotoWidgetUseCase: DuplicatePhotoWidgetUseCase,
    private val restoreWidgetUseCase: RestoreWidgetUseCase,
    private val savePhotoWidgetUseCase: SavePhotoWidgetUseCase,
    private val pinningCache: PhotoWidgetPinningCache,
    private val scope: CoroutineScope,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    private val duplicateFromId: Int? by savedStateHandle.savedState()
    private val restoreFromId: Int? by savedStateHandle.savedState()
    private val backupWidget: PhotoWidget? by savedStateHandle.savedState()
    private val aspectRatio: PhotoWidgetAspectRatio? by savedStateHandle.savedState()

    /**
     * The actual widget ID used for all storage operations. For new widgets, this is a unique
     * negative draft ID. For existing widgets (or drafts being continued), it matches [appWidgetId].
     */
    private var effectiveWidgetId: Int = appWidgetId

    private val _state: MutableStateFlow<PhotoWidgetConfigureState> = MutableStateFlow(PhotoWidgetConfigureState())
    val state: StateFlow<PhotoWidgetConfigureState> = _state.asStateFlow()

    init {
        Timber.i("Configuring widget (appWidgetId=$appWidgetId)")

        viewModelScope.launch {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                effectiveWidgetId = photoWidgetStorage.createNewDraftId()
                Timber.d("Assigned draft ID: $effectiveWidgetId")
            }

            if (photoWidgetStorage.getKnownWidgetIds().first().isNotEmpty()) {
                _state.update { current -> current.copy(isImportAvailable = true) }
            }

            val sourceWidget: PhotoWidget? = checkForSourceWidget(
                duplicateFromId = duplicateFromId,
                restoreFromId = restoreFromId,
                backupWidget = backupWidget,
            )

            if (sourceWidget != null) {
                val sanitized: PhotoWidget = sanitizeTapActionsUseCase(
                    appWidgetId = effectiveWidgetId,
                    photoWidget = sourceWidget,
                )

                updateState(photoWidget = sanitized, hasEdits = true)
            } else {
                loadPhotoWidgetUseCase(appWidgetId = effectiveWidgetId)
                    .onEach { photoWidget -> updateState(photoWidget = photoWidget, hasEdits = false) }
                    .onCompletion { throwable ->
                        if (throwable != null) return@onCompletion

                        val sanitized: PhotoWidget = sanitizeTapActionsUseCase(
                            appWidgetId = effectiveWidgetId,
                            photoWidget = state.value.photoWidget,
                        )
                        updateState(photoWidget = sanitized, hasEdits = false)

                        trackEdits()
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    private suspend fun checkForSourceWidget(
        duplicateFromId: Int?,
        restoreFromId: Int?,
        backupWidget: PhotoWidget?,
    ): PhotoWidget? {
        require(listOfNotNull(duplicateFromId, restoreFromId, backupWidget).size <= 1) {
            "Only one widget source must be provided."
        }

        return when {
            duplicateFromId != null -> {
                Timber.d("Duplicating widget (duplicateFromId=$duplicateFromId)")
                duplicatePhotoWidgetUseCase(originalAppWidgetId = duplicateFromId, newAppWidgetId = effectiveWidgetId)
            }

            restoreFromId != null -> {
                Timber.d("Restoring widget (restoreFromId=$restoreFromId)")
                duplicatePhotoWidgetUseCase(originalAppWidgetId = restoreFromId, newAppWidgetId = effectiveWidgetId)
            }

            backupWidget != null -> {
                Timber.d("Restoring widget from backup (backupWidget=$backupWidget)")
                runCatching { restoreWidgetUseCase(originalWidget = backupWidget, newAppWidgetId = effectiveWidgetId) }
                    .onFailure {
                        Timber.e(it, "Failed to restore widget from backup.")
                        _state += PhotoWidgetConfigureState.Message.MissingBackupData
                    }
                    .getOrNull()
            }

            else -> null
        }
    }

    private fun updateState(photoWidget: PhotoWidget, hasEdits: Boolean) {
        val resolvedAspectRatio: PhotoWidgetAspectRatio = aspectRatio ?: photoWidget.aspectRatio
        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = photoWidget.copy(
                    aspectRatio = resolvedAspectRatio,
                    cornerRadius = photoWidget.cornerRadius,
                ),
                selectedPhoto = photoWidget.currentPhoto ?: photoWidget.photos.firstOrNull(),
                isProcessing = photoWidget.isLoading,
                hasEdits = hasEdits,
                isDraft = PhotoWidget.isDraftWidgetId(effectiveWidgetId),
            )
        }
    }

    private fun trackEdits() {
        viewModelScope.launch {
            state.withIndex().first { (index, value) -> index > 0 && !value.hasEdits }
            _state.getAndUpdate { current -> current.copy(hasEdits = true) }
        }
    }

    fun importFromWidget(widgetId: Int) {
        viewModelScope.launch {
            _state.update { current -> current.copy(isProcessing = true) }

            val photoWidget = duplicatePhotoWidgetUseCase(
                originalAppWidgetId = widgetId,
                newAppWidgetId = effectiveWidgetId,
            )

            updateState(photoWidget = photoWidget, hasEdits = true)
        }
    }

    fun changeSource(newSource: PhotoWidgetSource) {
        photoWidgetStorage.saveWidgetSource(appWidgetId = effectiveWidgetId, source = newSource)
        photoWidgetStorage.loadWidgetPhotos(appWidgetId = effectiveWidgetId)
            .onEach { widgetPhotos ->
                _state.getAndUpdate { current ->
                    current.copy(
                        photoWidget = current.photoWidget.copy(
                            source = newSource,
                            photos = widgetPhotos.current,
                            currentPhoto = widgetPhotos.current.firstOrNull(),
                            tapActions = current.photoWidget.tapActions.coerceTapActions(source = newSource),
                            removedPhotos = widgetPhotos.excluded,
                        ),
                        selectedPhoto = widgetPhotos.current.firstOrNull(),
                        cropQueue = emptyList(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setAspectRatio(photoWidgetAspectRatio: PhotoWidgetAspectRatio) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    aspectRatio = photoWidgetAspectRatio,
                    shapeId = if (PhotoWidgetAspectRatio.SQUARE == photoWidgetAspectRatio) {
                        current.photoWidget.shapeId
                    } else {
                        PhotoWidget.DEFAULT_SHAPE_ID
                    },
                    cornerRadius = if (PhotoWidgetAspectRatio.SQUARE == photoWidgetAspectRatio ||
                        PhotoWidgetAspectRatio.FILL_WIDGET == photoWidgetAspectRatio
                    ) {
                        PhotoWidget.DEFAULT_CORNER_RADIUS
                    } else {
                        current.photoWidget.cornerRadius
                    },
                    border = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidgetAspectRatio) {
                        PhotoWidgetBorder.None
                    } else {
                        current.photoWidget.border
                    },
                ),
            )
        }
    }

    fun photoPicked(source: List<Uri>) {
        if (source.isEmpty()) return

        viewModelScope.launch {
            state.first { !it.isProcessing }

            _state.update { current -> current.copy(isProcessing = true) }

            val newPhotos = source.map { uri ->
                async {
                    photoWidgetStorage.newWidgetPhoto(
                        appWidgetId = effectiveWidgetId,
                        source = uri,
                    )
                }
            }.awaitAll().filterNotNull()

            val message = if (newPhotos.size < source.size) {
                PhotoWidgetConfigureState.Message.ImportFailed
            } else {
                null
            }

            val shouldTriggerCrop = newPhotos.isNotEmpty() && newPhotos.size <= 5

            _state.getAndUpdate { current ->
                current.copy(
                    isProcessing = false,
                    cropQueue = if (shouldTriggerCrop) newPhotos else emptyList(),
                ) + newPhotos + message
            }

            if (shouldTriggerCrop) {
                requestCrop(photo = newPhotos.first())
            }
        }
    }

    fun dirPicked(source: Uri?) {
        if (source == null) return

        viewModelScope.launch {
            _state.update { current -> current.copy(isProcessing = true) }

            val newDirPhotos = photoWidgetStorage.getNewDirPhotos(
                dirUri = source,
                sorting = _state.value.photoWidget.directorySorting,
            )
            if (newDirPhotos == null) {
                _state.update { current ->
                    current.copy(isProcessing = false) + PhotoWidgetConfigureState.Message.TooManyPhotos
                }

                return@launch
            }

            val updatedState = _state.getAndUpdate { current ->
                current.copy(isProcessing = false, cropQueue = emptyList()) + source + newDirPhotos
            }
            photoWidgetStorage.saveWidgetSyncedDir(
                appWidgetId = effectiveWidgetId,
                dirUri = updatedState.photoWidget.syncedDir,
            )
        }
    }

    fun removeDir(source: Uri) {
        viewModelScope.launch {
            photoWidgetStorage.removeSyncedDir(appWidgetId = effectiveWidgetId, dirUri = source)

            reloadDirPhotos(syncedDir = _state.value.photoWidget.syncedDir - source)
        }
    }

    private fun reloadDirPhotos(syncedDir: Collection<Uri>) {
        photoWidgetStorage.loadWidgetPhotos(appWidgetId = effectiveWidgetId)
            .onEach { widgetPhotos ->
                _state.getAndUpdate { current ->
                    current.copy(
                        photoWidget = current.photoWidget.copy(
                            photos = widgetPhotos.current,
                            currentPhoto = widgetPhotos.current.firstOrNull(),
                            syncedDir = syncedDir.toSet(),
                            removedPhotos = widgetPhotos.excluded,
                        ),
                        selectedPhoto = current.selectedPhoto ?: widgetPhotos.current.firstOrNull(),
                        isProcessing = false,
                        cropQueue = emptyList(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun previewPhoto(photo: LocalPhoto) {
        _state.getAndUpdate { current -> current.copy(selectedPhoto = photo) }
    }

    fun requestCrop(photo: LocalPhoto) {
        viewModelScope.launch {
            val (source, destination) = photoWidgetStorage.getCropSources(
                appWidgetId = effectiveWidgetId,
                localPhoto = photo,
            )

            _state.getAndUpdate { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = current.photoWidget.photos.map { it.copy(cropping = it.photoId == photo.photoId) },
                    ),
                ) + PhotoWidgetConfigureState.Message.LaunchCrop(
                    source = source,
                    destination = destination,
                    aspectRatio = current.photoWidget.aspectRatio,
                )
            }
        }
    }

    fun photoCropped(path: String) {
        val cropping = _state.value.photoWidget.photos.firstOrNull { it.cropping } ?: return
        val currentTimeMillis = System.currentTimeMillis()
        val updateMatchingPhoto = { photo: LocalPhoto ->
            if (photo.photoId == cropping.photoId) {
                photo.copy(
                    croppedPhotoPath = path,
                    cropping = false,
                    timestamp = currentTimeMillis,
                )
            } else {
                photo
            }
        }

        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.map(updateMatchingPhoto),
                    currentPhoto = current.photoWidget.currentPhoto?.let(updateMatchingPhoto),
                ),
                selectedPhoto = current.selectedPhoto?.let(updateMatchingPhoto),
                cropQueue = current.cropQueue.filterNot { it.photoId == cropping.photoId },
            )
        }

        val cropQueue = _state.value.cropQueue
        if (cropQueue.isNotEmpty()) {
            requestCrop(photo = cropQueue.first())
        }
    }

    fun cropCancelled() {
        _state.update { current -> current.copy(cropQueue = emptyList()) }
    }

    fun removePhoto(photo: LocalPhoto) {
        _state.getAndUpdate { current ->
            val removedPhoto = current.photoWidget.photos.firstOrNull { it.photoId == photo.photoId }
            val updatedPhotos = current.photoWidget.photos.filterNot { it.photoId == photo.photoId }
            val newIndex = current.photoWidget.photos.indexOfFirst { it.photoId == photo.photoId }
                .coerceAtMost(updatedPhotos.size - 1)

            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = updatedPhotos,
                    currentPhoto = if (current.photoWidget.currentPhoto?.photoId == photo.photoId) {
                        updatedPhotos.getOrNull(newIndex)
                    } else {
                        current.photoWidget.currentPhoto
                    },
                    removedPhotos = current.photoWidget.removedPhotos.let { removedPhotos ->
                        if (removedPhoto != null) removedPhotos + removedPhoto else removedPhotos
                    },
                ),
                selectedPhoto = if (current.selectedPhoto?.photoId == photo.photoId) {
                    updatedPhotos.getOrNull(newIndex)
                } else {
                    current.selectedPhoto
                },
            )
        }
    }

    fun restorePhoto(photo: LocalPhoto) {
        _state.getAndUpdate { current -> current + photo }
    }

    fun deletePhotoPermanently(photo: LocalPhoto) {
        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    removedPhotos = current.photoWidget.removedPhotos.filterNot { it.photoId == photo.photoId },
                ),
            )
        }
    }

    fun moveLeft(photo: LocalPhoto) {
        move {
            val currentIndex = indexOfFirst { it.photoId == photo.photoId }
            val newIndex = currentIndex.minus(1).coerceAtLeast(0)

            add(newIndex, removeAt(currentIndex))
        }
    }

    fun moveRight(photo: LocalPhoto) {
        move {
            val currentIndex = indexOfFirst { it.photoId == photo.photoId }
            val newIndex = currentIndex.plus(1).coerceAtMost(size - 1)

            add(newIndex, removeAt(currentIndex))
        }
    }

    private fun move(moveOp: MutableList<LocalPhoto>.() -> Unit) {
        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.toMutableList().apply(moveOp),
                ),
            )
        }
    }

    fun reorderPhotos(photos: List<LocalPhoto>) {
        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = photos,
                ),
            )
        }
    }

    fun cycleModeSelected(cycleMode: PhotoWidgetCycleMode) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(cycleMode = cycleMode))
        }
    }

    fun saveShuffle(value: Boolean) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(shuffle = value))
        }
    }

    fun saveSorting(sorting: DirectorySorting) {
        viewModelScope.launch {
            val updatedPhotos: List<LocalPhoto> = withContext(Dispatchers.Default) {
                _state.value.photoWidget.photos.let { photos ->
                    when (sorting) {
                        DirectorySorting.NEWEST_FIRST -> photos.sortedByDescending { it.timestamp }
                        DirectorySorting.OLDEST_FIRST -> photos.sortedBy { it.timestamp }
                    }
                }
            }

            _state.update { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = updatedPhotos,
                        directorySorting = sorting,
                    ),
                )
            }
        }
    }

    fun tapActionSelected(tapActions: PhotoWidgetTapActions) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(tapActions = tapActions))
        }
    }

    fun shapeSelected(shapeId: String) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(shapeId = shapeId))
        }
    }

    fun cornerRadiusSelected(cornerRadius: Int) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(cornerRadius = cornerRadius))
        }
    }

    fun borderSelected(border: PhotoWidgetBorder) {
        _state.update { current ->
            current.copy(photoWidget = current.photoWidget.copy(border = border))
        }
    }

    fun opacitySelected(opacity: Float) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    colors = current.photoWidget.colors.copy(opacity = opacity),
                ),
            )
        }
    }

    fun saturationSelected(saturation: Float) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    colors = current.photoWidget.colors.copy(saturation = saturation),
                ),
            )
        }
    }

    fun brightnessSelected(brightness: Float) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    colors = current.photoWidget.colors.copy(brightness = brightness),
                ),
            )
        }
    }

    fun offsetSelected(horizontalOffset: Int, verticalOffset: Int) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    horizontalOffset = horizontalOffset,
                    verticalOffset = verticalOffset,
                ),
            )
        }
    }

    fun paddingSelected(padding: Int) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    padding = padding,
                ),
            )
        }
    }

    fun photoWidgetTextChanged(photoWidgetText: PhotoWidgetText) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    text = photoWidgetText,
                ),
            )
        }
    }

    fun addNewWidget() {
        val currentState = _state.value

        when {
            // Without photos there's no widget
            currentState.photoWidget.photos.isEmpty() && currentState.isDraft -> {
                _state += PhotoWidgetConfigureState.Message.CancelWidget
            }

            currentState.photoWidget.photos.isEmpty() -> {
                _state += PhotoWidgetConfigureState.Message.MissingPhotos
            }

            // The user started configuring from within the app, request to pin, but they might cancel
            currentState.isDraft -> {
                pinningCache.populate(currentState.photoWidget, draftWidgetId = effectiveWidgetId)

                _state += PhotoWidgetConfigureState.Message.RequestPin
            }

            // The user started configuring from the home screen, it will be added automatically
            else -> {
                viewModelScope.launch {
                    savePhotoWidgetUseCase(
                        draftWidgetId = effectiveWidgetId,
                        appWidgetId = appWidgetId,
                        photoWidget = currentState.photoWidget,
                    )

                    _state += PhotoWidgetConfigureState.Message.AddWidget(appWidgetId = appWidgetId)
                }
            }
        }
    }

    fun widgetAdded() {
        scope.launch {
            withContext(NonCancellable) {
                restoreFromId?.let { photoWidgetStorage.deleteWidgetData(appWidgetId = it) }
            }
        }
    }

    fun saveDraft() {
        scope.launch {
            withContext(NonCancellable) {
                savePhotoWidgetUseCase(
                    draftWidgetId = effectiveWidgetId,
                    appWidgetId = effectiveWidgetId,
                    photoWidget = state.value.photoWidget,
                )
            }
        }
    }

    fun discardDraft() {
        scope.launch {
            withContext(NonCancellable) {
                photoWidgetStorage.deleteWidgetData(appWidgetId = effectiveWidgetId)
            }
        }
    }

    fun messageHandled(message: PhotoWidgetConfigureState.Message) {
        _state -= message
    }
}
