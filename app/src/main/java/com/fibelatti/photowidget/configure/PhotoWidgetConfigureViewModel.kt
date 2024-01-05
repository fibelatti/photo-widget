package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoWidgetConfigureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val savePhotoWidgetUseCase: SavePhotoWidgetUseCase,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    private val aspectRatio: PhotoWidgetAspectRatio? by savedStateHandle.savedState()

    private val _state = MutableStateFlow(PhotoWidgetConfigureState())
    val state: StateFlow<PhotoWidgetConfigureState> = _state.asStateFlow()

    init {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Delete stale data from a widget that wasn't placed
            photoWidgetStorage.deleteWidgetData(appWidgetId = appWidgetId)
        }

        val savedPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
        val savedInterval = photoWidgetStorage.getWidgetInterval(appWidgetId = appWidgetId)
        val savedAspectRatio = photoWidgetStorage.getWidgetAspectRatio(appWidgetId = appWidgetId)
        val savedShapeId = photoWidgetStorage.getWidgetShapeId(appWidgetId = appWidgetId)

        _state.update { current ->
            current.copy(
                photos = savedPhotos,
                selectedPhoto = savedPhotos.firstOrNull(),
                loopingInterval = savedInterval ?: current.loopingInterval,
                aspectRatio = aspectRatio ?: savedAspectRatio,
                shapeId = savedShapeId ?: current.shapeId,
            )
        }
    }

    fun photoPicked(source: List<Uri>) {
        if (source.isEmpty()) return

        _state.update { current -> current.copy(isProcessing = true) }

        viewModelScope.launch {
            val newPhotos = source.map { uri ->
                async {
                    photoWidgetStorage.newWidgetPhoto(
                        appWidgetId = appWidgetId,
                        source = uri,
                    )
                }
            }.awaitAll().filterNotNull()

            _state.update { current ->
                val updatedPhotos = current.photos + newPhotos

                current.copy(
                    photos = updatedPhotos,
                    selectedPhoto = current.selectedPhoto ?: updatedPhotos.firstOrNull(),
                    isProcessing = false,
                )
            }

            if (newPhotos.size == 1) {
                requestCrop(photo = newPhotos.first())
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
                photoName = photo.name,
            )

            _state.update { current ->
                current.copy(
                    message = PhotoWidgetConfigureState.Message.LaunchCrop(
                        source = Uri.fromFile(source),
                        destination = Uri.fromFile(destination),
                        aspectRatio = current.aspectRatio,
                    ),
                )
            }
        }
    }

    fun photoCropped(path: String) {
        _state.update { current ->
            current.copy(
                photos = current.photos.map { photo ->
                    if (photo.path == path) {
                        photo.copy(
                            isCropped = true,
                            timestamp = System.currentTimeMillis(),
                        )
                    } else {
                        photo
                    }
                },
            )
        }
    }

    fun photoRemoved(photo: LocalPhoto) {
        photoWidgetStorage.deleteWidgetPhoto(
            appWidgetId = appWidgetId,
            photoName = photo.name,
        )
        _state.update { current ->
            val updatedPhotos = current.photos - photo
            current.copy(
                photos = updatedPhotos,
                selectedPhoto = if (current.selectedPhoto?.name == photo.name) {
                    updatedPhotos.firstOrNull()
                } else {
                    current.selectedPhoto
                },
            )
        }
    }

    fun moveLeft(photo: LocalPhoto) {
        move {
            val currentIndex = indexOf(photo)
            val newIndex = currentIndex.minus(1).coerceAtLeast(0)

            add(newIndex, removeAt(currentIndex))
        }
    }

    fun moveRight(photo: LocalPhoto) {
        move {
            val currentIndex = indexOf(photo)
            val newIndex = currentIndex.plus(1).coerceAtMost(size - 1)

            add(newIndex, removeAt(currentIndex))
        }
    }

    private fun move(moveOp: MutableList<LocalPhoto>.() -> Unit) {
        _state.update { current ->
            current.copy(
                photos = current.photos.toMutableList().apply(moveOp),
            )
        }
    }

    fun intervalSelected(interval: PhotoWidgetLoopingInterval) {
        _state.update { current -> current.copy(loopingInterval = interval) }
    }

    fun shapeSelected(shapeId: String) {
        _state.update { current -> current.copy(shapeId = shapeId) }
    }

    fun addNewWidget() {
        val currentState = _state.value

        when {
            // Without photos there's no widget
            currentState.photos.isEmpty() -> {
                _state.update { current ->
                    current.copy(message = PhotoWidgetConfigureState.Message.CancelWidget)
                }
            }

            // The user started configuring from within the app, request to pin but they might cancel
            AppWidgetManager.INVALID_APPWIDGET_ID == appWidgetId -> {
                _state.update { current ->
                    current.copy(
                        message = PhotoWidgetConfigureState.Message.RequestPin(
                            photoPath = currentState.photos.first().path,
                            order = currentState.photos.map { it.name },
                            enableLooping = currentState.photos.size > 1,
                            loopingInterval = currentState.loopingInterval,
                            aspectRatio = current.aspectRatio,
                            shapeId = currentState.shapeId,
                        ),
                    )
                }
            }

            // The user start configuring from the home screen, it will be added automatically
            else -> {
                savePhotoWidgetUseCase(
                    appWidgetId = appWidgetId,
                    order = currentState.photos.map { it.name },
                    enableLooping = currentState.photos.size > 1,
                    loopingInterval = currentState.loopingInterval,
                    aspectRatio = currentState.aspectRatio,
                    shapeId = currentState.shapeId,
                )

                _state.update { current ->
                    current.copy(
                        message = PhotoWidgetConfigureState.Message.AddWidget(
                            appWidgetId = appWidgetId,
                            photoPath = currentState.photos.first().path,
                            aspectRatio = current.aspectRatio,
                            shapeId = currentState.shapeId,
                        ),
                    )
                }
            }
        }
    }

    fun messageHandled() {
        _state.update { current -> current.copy(message = null) }
    }
}
