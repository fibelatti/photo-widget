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
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.DeleteStaleDataUseCase
import com.fibelatti.photowidget.widget.DuplicatePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.RestoreWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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
    private val duplicatePhotoWidgetUseCase: DuplicatePhotoWidgetUseCase,
    private val restoreWidgetUseCase: RestoreWidgetUseCase,
    private val savePhotoWidgetUseCase: SavePhotoWidgetUseCase,
    deleteStaleDataUseCase: DeleteStaleDataUseCase,
    private val pinningCache: PhotoWidgetPinningCache,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    private val duplicateFromId: Int? by savedStateHandle.savedState()
    private val restoreFromId: Int? by savedStateHandle.savedState()
    private val backupWidget: PhotoWidget? by savedStateHandle.savedState()
    private val aspectRatio: PhotoWidgetAspectRatio? by savedStateHandle.savedState()

    private val _state = MutableStateFlow(PhotoWidgetConfigureState())
    val state: StateFlow<PhotoWidgetConfigureState> = _state.asStateFlow()

    init {
        Timber.d("Configuring widget (appWidgetId=$appWidgetId)")

        viewModelScope.launch {
            deleteStaleDataUseCase()

            val sourceWidget: PhotoWidget? = checkForSourceWidget(
                duplicateFromId = duplicateFromId,
                restoreFromId = restoreFromId,
                backupWidget = backupWidget,
            )

            if (sourceWidget != null) {
                updateState(photoWidget = sourceWidget, hasEdits = true)
            } else {
                loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                    .onEach { photoWidget -> updateState(photoWidget = photoWidget, hasEdits = false) }
                    .onCompletion { trackEdits() }
                    .launchIn(viewModelScope)
            }

            checkImportSuggestion()
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
                duplicatePhotoWidgetUseCase(originalAppWidgetId = duplicateFromId, newAppWidgetId = appWidgetId)
            }

            restoreFromId != null -> {
                Timber.d("Restoring widget (restoreFromId=$restoreFromId)")
                duplicatePhotoWidgetUseCase(originalAppWidgetId = restoreFromId, newAppWidgetId = appWidgetId)
            }

            backupWidget != null -> {
                Timber.d("Restoring widget from backup (backupWidget=$backupWidget)")
                runCatching { restoreWidgetUseCase(originalWidget = backupWidget, newAppWidgetId = appWidgetId) }
                    .onFailure {
                        Timber.e(it, "Failed to restore widget from backup.")
                        _state.update { current ->
                            current.copy(
                                messages = current.messages + PhotoWidgetConfigureState.Message.MissingBackupData,
                            )
                        }
                    }
                    .getOrNull()
            }

            else -> null
        }
    }

    private fun updateState(photoWidget: PhotoWidget, hasEdits: Boolean) {
        val resolvedAspectRatio = aspectRatio ?: photoWidget.aspectRatio
        _state.getAndUpdate { current ->
            current.copy(
                photoWidget = photoWidget.copy(
                    aspectRatio = resolvedAspectRatio,
                    cornerRadius = photoWidget.cornerRadius,
                ),
                selectedPhoto = photoWidget.currentPhoto ?: photoWidget.photos.firstOrNull(),
                isProcessing = photoWidget.isLoading,
                hasEdits = hasEdits,
            )
        }
    }

    private fun trackEdits() {
        viewModelScope.launch {
            state.withIndex().first { (index, value) -> index > 0 && !value.hasEdits }
            _state.getAndUpdate { current -> current.copy(hasEdits = true) }
        }
    }

    private fun checkImportSuggestion() {
        viewModelScope.launch {
            val currentPhotos = state.first { !it.isProcessing }.photoWidget.photos
            val canImport = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                currentPhotos.isEmpty() &&
                photoWidgetStorage.getKnownWidgetIds().isNotEmpty()

            if (canImport) {
                delay(1_000)
                _state.update { current ->
                    current.copy(messages = current.messages + PhotoWidgetConfigureState.Message.SuggestImport)
                }
            }
        }
    }

    fun importFromWidget(widgetId: Int) {
        viewModelScope.launch {
            _state.update { current -> current.copy(isProcessing = true) }

            val photoWidget = duplicatePhotoWidgetUseCase(
                originalAppWidgetId = widgetId,
                newAppWidgetId = appWidgetId,
            )

            updateState(photoWidget = photoWidget, hasEdits = true)
        }
    }

    fun changeSource() {
        val newSource = when (_state.value.photoWidget.source) {
            PhotoWidgetSource.PHOTOS -> PhotoWidgetSource.DIRECTORY
            PhotoWidgetSource.DIRECTORY -> PhotoWidgetSource.PHOTOS
        }

        photoWidgetStorage.saveWidgetSource(appWidgetId = appWidgetId, source = newSource)

        photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
            .onEach { widgetPhotos ->
                _state.update { current ->
                    current.copy(
                        photoWidget = current.photoWidget.copy(
                            source = newSource,
                            photos = widgetPhotos.current,
                            currentPhoto = widgetPhotos.current.firstOrNull(),
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
            while (_state.value.isProcessing) delay(timeMillis = 100L)

            _state.update { current -> current.copy(isProcessing = true) }

            val newPhotos = source.map { uri ->
                async {
                    photoWidgetStorage.newWidgetPhoto(
                        appWidgetId = appWidgetId,
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

            _state.update { current ->
                val updatedPhotos = current.photoWidget.photos + newPhotos

                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = updatedPhotos,
                        currentPhoto = current.photoWidget.currentPhoto ?: updatedPhotos.firstOrNull(),
                    ),
                    selectedPhoto = current.selectedPhoto ?: updatedPhotos.firstOrNull(),
                    isProcessing = false,
                    cropQueue = if (shouldTriggerCrop) newPhotos else emptyList(),
                    messages = current.messages.plus(message).filterNotNull(),
                )
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
                    current.copy(
                        isProcessing = false,
                        messages = current.messages + PhotoWidgetConfigureState.Message.TooManyPhotos,
                    )
                }

                return@launch
            }

            val syncedDir = _state.value.photoWidget.syncedDir + source

            photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = syncedDir)

            _state.update { current ->
                val updatedPhotos = current.photoWidget.photos + newDirPhotos

                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = updatedPhotos,
                        currentPhoto = current.photoWidget.currentPhoto ?: updatedPhotos.firstOrNull(),
                        syncedDir = syncedDir.toSet(),
                        removedPhotos = current.photoWidget.removedPhotos,
                    ),
                    selectedPhoto = current.selectedPhoto ?: updatedPhotos.firstOrNull(),
                    isProcessing = false,
                    cropQueue = emptyList(),
                )
            }
        }
    }

    fun removeDir(source: Uri) {
        viewModelScope.launch {
            photoWidgetStorage.removeSyncedDir(appWidgetId = appWidgetId, dirUri = source)

            reloadDirPhotos(syncedDir = _state.value.photoWidget.syncedDir - source)
        }
    }

    private fun reloadDirPhotos(syncedDir: Collection<Uri>) {
        photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
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
                appWidgetId = appWidgetId,
                localPhoto = photo,
            )

            _state.getAndUpdate { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = current.photoWidget.photos.map { it.copy(cropping = it.photoId == photo.photoId) },
                    ),
                    messages = current.messages + PhotoWidgetConfigureState.Message.LaunchCrop(
                        source = source,
                        destination = destination,
                        aspectRatio = current.photoWidget.aspectRatio,
                    ),
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

    fun photoRemoved(photo: LocalPhoto) {
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
        _state.getAndUpdate { current ->
            val updatedPhotos = current.photoWidget.photos + photo
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = updatedPhotos,
                    currentPhoto = if (updatedPhotos.size == 1) {
                        updatedPhotos.firstOrNull()
                    } else {
                        current.photoWidget.currentPhoto
                    },
                    removedPhotos = current.photoWidget.removedPhotos.filterNot { it.photoId == photo.photoId },
                ),
                selectedPhoto = if (updatedPhotos.size == 1) {
                    updatedPhotos.firstOrNull()
                } else {
                    current.selectedPhoto
                },
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
            current.copy(
                photoWidget = current.photoWidget.copy(shuffle = value),
            )
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
            current.copy(
                photoWidget = current.photoWidget.copy(tapActions = tapActions),
            )
        }
    }

    fun shapeSelected(shapeId: String) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(shapeId = shapeId),
            )
        }
    }

    fun cornerRadiusSelected(cornerRadius: Int) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(cornerRadius = cornerRadius),
            )
        }
    }

    fun borderSelected(border: PhotoWidgetBorder) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(border = border),
            )
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

    fun addNewWidget() {
        val currentState = _state.value

        when {
            // Without photos there's no widget
            currentState.photoWidget.photos.isEmpty() && AppWidgetManager.INVALID_APPWIDGET_ID == appWidgetId -> {
                _state.update { current ->
                    current.copy(messages = current.messages + PhotoWidgetConfigureState.Message.CancelWidget)
                }
            }

            currentState.photoWidget.photos.isEmpty() -> {
                _state.update { current ->
                    current.copy(messages = current.messages + PhotoWidgetConfigureState.Message.MissingPhotos)
                }
            }

            // The user started configuring from within the app, request to pin but they might cancel
            AppWidgetManager.INVALID_APPWIDGET_ID == appWidgetId -> {
                pinningCache.populate(currentState.photoWidget)

                _state.update { current ->
                    current.copy(
                        messages = current.messages + PhotoWidgetConfigureState.Message.RequestPin,
                    )
                }
            }

            // The user start configuring from the home screen, it will be added automatically
            else -> {
                viewModelScope.launch {
                    savePhotoWidgetUseCase(
                        appWidgetId = appWidgetId,
                        photoWidget = currentState.photoWidget,
                    )

                    _state.update { current ->
                        current.copy(
                            messages = current.messages + PhotoWidgetConfigureState.Message.AddWidget(
                                appWidgetId = appWidgetId,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun widgetAdded() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                restoreFromId?.let { photoWidgetStorage.deleteWidgetData(appWidgetId = it) }
            }
        }
    }

    fun messageHandled(message: PhotoWidgetConfigureState.Message) {
        _state.update { current -> current.copy(messages = current.messages - message) }
    }
}
