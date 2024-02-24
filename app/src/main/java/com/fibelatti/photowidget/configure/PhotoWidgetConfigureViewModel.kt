package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
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
    loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
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

        val photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)

        _state.update { current ->
            current.copy(
                photoWidget = photoWidget.copy(aspectRatio = aspectRatio ?: photoWidget.aspectRatio),
                selectedPhoto = photoWidget.photos.firstOrNull(),
            )
        }
    }

    fun setAspectRatio(photoWidgetAspectRatio: PhotoWidgetAspectRatio) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    aspectRatio = photoWidgetAspectRatio,
                    shapeId = if (photoWidgetAspectRatio == PhotoWidgetAspectRatio.SQUARE) {
                        current.photoWidget.shapeId
                    } else {
                        PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID
                    },
                ),
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

            val message = if (newPhotos.size < source.size) {
                PhotoWidgetConfigureState.Message.ImportFailed
            } else {
                null
            }

            _state.update { current ->
                val updatedPhotos = current.photoWidget.photos + newPhotos

                current.copy(
                    photoWidget = current.photoWidget.copy(photos = updatedPhotos),
                    selectedPhoto = current.selectedPhoto ?: updatedPhotos.firstOrNull(),
                    isProcessing = false,
                    cropQueue = newPhotos,
                    messages = current.messages.plus(message).filterNotNull(),
                )
            }

            if (newPhotos.isNotEmpty()) {
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
                    messages = current.messages + PhotoWidgetConfigureState.Message.LaunchCrop(
                        source = Uri.fromFile(source),
                        destination = Uri.fromFile(destination),
                        aspectRatio = current.photoWidget.aspectRatio,
                    ),
                )
            }
        }
    }

    fun photoCropped(path: String) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.map { photo ->
                        if (photo.path == path) {
                            photo.copy(timestamp = System.currentTimeMillis())
                        } else {
                            photo
                        }
                    },
                ),
                selectedPhoto = if (current.selectedPhoto?.path == path) {
                    current.selectedPhoto.copy(timestamp = System.currentTimeMillis())
                } else {
                    current.selectedPhoto
                },
                cropQueue = current.cropQueue.filterNot { it.path == path },
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
        photoWidgetStorage.deleteWidgetPhoto(
            appWidgetId = appWidgetId,
            photoName = photo.name,
        )
        _state.update { current ->
            val updatedPhotos = current.photoWidget.photos - photo
            current.copy(
                photoWidget = current.photoWidget.copy(photos = updatedPhotos),
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
                photoWidget = current.photoWidget.copy(
                    photos = current.photoWidget.photos.toMutableList().apply(moveOp),
                ),
            )
        }
    }

    fun intervalSelected(interval: PhotoWidgetLoopingInterval) {
        _state.update { current ->
            current.copy(
                photoWidget = current.photoWidget.copy(loopingInterval = interval),
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

    fun addNewWidget() {
        val currentState = _state.value

        when {
            // Without photos there's no widget
            currentState.photoWidget.photos.isEmpty() -> {
                _state.update { current ->
                    current.copy(messages = current.messages + PhotoWidgetConfigureState.Message.CancelWidget)
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

    fun messageHandled(message: PhotoWidgetConfigureState.Message) {
        _state.update { current -> current.copy(messages = current.messages - message) }
    }
}
