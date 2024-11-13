package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.DeleteStaleDataUseCase
import com.fibelatti.photowidget.widget.DuplicatePhotoWidgetUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    duplicatePhotoWidgetUseCase: DuplicatePhotoWidgetUseCase,
    private val savePhotoWidgetUseCase: SavePhotoWidgetUseCase,
    deleteStaleDataUseCase: DeleteStaleDataUseCase,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    private val duplicateFromId: Int? by savedStateHandle.savedState()
    private val restoreFromId: Int? by savedStateHandle.savedState()
    private val aspectRatio: PhotoWidgetAspectRatio? by savedStateHandle.savedState()

    private val _state = MutableStateFlow(PhotoWidgetConfigureState())
    val state: StateFlow<PhotoWidgetConfigureState> = _state.asStateFlow()

    init {
        Timber.d("Configuring widget (appWidgetId=$appWidgetId)")

        viewModelScope.launch {
            deleteStaleDataUseCase()

            val sourceWidget = duplicateFromId?.let {
                Timber.d("Duplicating widget (duplicateFromId=$it)")
                duplicatePhotoWidgetUseCase(originalAppWidgetId = it, newAppWidgetId = appWidgetId)
            } ?: restoreFromId?.let {
                Timber.d("Restoring widget (restoreFromId=$it)")
                duplicatePhotoWidgetUseCase(originalAppWidgetId = it, newAppWidgetId = appWidgetId)
            }
            val updateState = { photoWidget: PhotoWidget ->
                val resolvedAspectRatio = aspectRatio ?: photoWidget.aspectRatio
                _state.update { current ->
                    current.copy(
                        photoWidget = photoWidget.copy(
                            aspectRatio = resolvedAspectRatio,
                            cornerRadius = if (PhotoWidgetAspectRatio.FILL_WIDGET == resolvedAspectRatio) {
                                0F
                            } else {
                                PhotoWidget.DEFAULT_CORNER_RADIUS
                            },
                        ),
                        selectedPhoto = photoWidget.photos.firstOrNull(),
                        isProcessing = photoWidget.isLoading,
                        hasEdits = sourceWidget != null,
                    )
                }
            }

            if (sourceWidget != null) {
                updateState(sourceWidget)
            } else {
                loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                    .onEach { photoWidget -> updateState(photoWidget) }
                    .onCompletion { trackEdits() }
                    .launchIn(viewModelScope)
            }
        }
    }

    private fun trackEdits() {
        viewModelScope.launch {
            state.withIndex().first { (index, value) -> index > 0 && !value.hasEdits }
            _state.update { current -> current.copy(hasEdits = true) }
        }
    }

    fun changeSource() {
        val newSource = when (_state.value.photoWidget.source) {
            PhotoWidgetSource.PHOTOS -> PhotoWidgetSource.DIRECTORY
            PhotoWidgetSource.DIRECTORY -> PhotoWidgetSource.PHOTOS
        }

        photoWidgetStorage.saveWidgetSource(appWidgetId = appWidgetId, source = newSource)

        viewModelScope.launch {
            val photos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)

            _state.update { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        source = newSource,
                        photos = photos,
                    ),
                    selectedPhoto = photos.firstOrNull(),
                    cropQueue = emptyList(),
                )
            }
        }
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
                    cornerRadius = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidgetAspectRatio) {
                        0F
                    } else {
                        PhotoWidget.DEFAULT_CORNER_RADIUS
                    },
                    borderColor = if (PhotoWidgetAspectRatio.FILL_WIDGET == photoWidgetAspectRatio) {
                        null
                    } else {
                        current.photoWidget.borderColor
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
                    photoWidget = current.photoWidget.copy(photos = updatedPhotos),
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

            if (!photoWidgetStorage.isValidDir(dirUri = source)) {
                _state.update { current ->
                    current.copy(
                        isProcessing = false,
                        messages = current.messages + PhotoWidgetConfigureState.Message.TooManyPhotos,
                    )
                }

                return@launch
            }

            val dirList = _state.value.photoWidget.syncedDir + source

            photoWidgetStorage.saveWidgetSyncedDir(appWidgetId = appWidgetId, dirUri = dirList)

            val photos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)

            _state.update { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = photos,
                        syncedDir = dirList,
                    ),
                    selectedPhoto = photos.firstOrNull(),
                    isProcessing = false,
                    cropQueue = emptyList(),
                )
            }
        }
    }

    fun removeDir(source: Uri) {
        viewModelScope.launch {
            photoWidgetStorage.removeSyncedDir(appWidgetId = appWidgetId, dirUri = source)

            val photos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)

            _state.update { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = photos,
                        syncedDir = _state.value.photoWidget.syncedDir - source,
                    ),
                    selectedPhoto = photos.firstOrNull(),
                    isProcessing = false,
                    cropQueue = emptyList(),
                )
            }
        }
    }

    fun previewPhoto(photo: LocalPhoto) {
        _state.update { current -> current.copy(selectedPhoto = photo) }
    }

    fun requestCrop(photo: LocalPhoto) {
        viewModelScope.launch {
            val (source, destination) = photoWidgetStorage.getCropSources(
                appWidgetId = appWidgetId,
                localPhoto = photo,
            )

            _state.update { current ->
                current.copy(
                    photoWidget = current.photoWidget.copy(
                        photos = current.photoWidget.photos.map { it.copy(cropping = it.name == photo.name) },
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

        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.map { photo ->
                        if (photo.name == cropping.name) {
                            photo.copy(
                                path = path,
                                cropping = false,
                                timestamp = System.currentTimeMillis(),
                            )
                        } else {
                            photo
                        }
                    },
                ),
                selectedPhoto = if (current.selectedPhoto?.name == cropping.name) {
                    current.selectedPhoto.copy(
                        path = path,
                        timestamp = System.currentTimeMillis(),
                    )
                } else {
                    current.selectedPhoto
                },
                cropQueue = current.cropQueue.filterNot { it.name == cropping.name },
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
        _state.update { current ->
            val removedPhoto = current.photoWidget.photos.first { it.name == photo.name }
            val updatedPhotos = current.photoWidget.photos.filterNot { it.name == photo.name }
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = updatedPhotos,
                    photosPendingDeletion = current.photoWidget.photosPendingDeletion + removedPhoto,
                ),
                selectedPhoto = if (current.selectedPhoto?.name == photo.name) {
                    updatedPhotos.firstOrNull()
                } else {
                    current.selectedPhoto
                },
                markedForDeletion = current.markedForDeletion + photo.name,
            )
        }
    }

    fun restorePhoto(photo: LocalPhoto) {
        _state.update { current ->
            val updatedPhotos = current.photoWidget.photos + photo
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = updatedPhotos,
                    photosPendingDeletion = current.photoWidget.photosPendingDeletion.filterNot { it.name == photo.name },
                ),
                selectedPhoto = if (updatedPhotos.size == 1) {
                    updatedPhotos.firstOrNull()
                } else {
                    current.selectedPhoto
                },
                markedForDeletion = current.markedForDeletion - photo.name,
            )
        }
    }

    fun moveLeft(photo: LocalPhoto) {
        move {
            val currentIndex = indexOfFirst { it.name == photo.name }
            val newIndex = currentIndex.minus(1).coerceAtLeast(0)

            add(newIndex, removeAt(currentIndex))
        }
    }

    fun moveRight(photo: LocalPhoto) {
        move {
            val currentIndex = indexOfFirst { it.name == photo.name }
            val newIndex = currentIndex.plus(1).coerceAtMost(size - 1)

            add(newIndex, removeAt(currentIndex))
        }
    }

    private fun move(moveOp: MutableList<LocalPhoto>.() -> Unit) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.toMutableList().apply(moveOp),
                ),
            )
        }
    }

    fun reorderPhotos(photos: List<LocalPhoto>) {
        _state.update { current ->
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

    fun toggleShuffle() {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    shuffle = !current.photoWidget.shuffle,
                ),
            )
        }
    }

    fun tapActionSelected(tapAction: PhotoWidgetTapAction) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(tapAction = tapAction),
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

    fun cornerRadiusSelected(cornerRadius: Float) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(cornerRadius = cornerRadius),
            )
        }
    }

    fun borderSelected(colorHex: String?, width: Int) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    borderColor = colorHex,
                    borderWidth = width,
                ),
            )
        }
    }

    fun opacitySelected(opacity: Float) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(opacity = opacity),
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
                _state.update { current ->
                    current.copy(
                        messages = current.messages + PhotoWidgetConfigureState.Message.RequestPin(
                            photoWidget = currentState.photoWidget,
                        ),
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

                photoWidgetStorage.markPhotosForDeletion(
                    appWidgetId = appWidgetId,
                    photoNames = _state.value.markedForDeletion,
                )
            }
        }
    }

    fun messageHandled(message: PhotoWidgetConfigureState.Message) {
        _state.update { current -> current.copy(messages = current.messages - message) }
    }
}
