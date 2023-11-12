package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
                loopingInterval = savedInterval ?: current.loopingInterval,
                aspectRatio = aspectRatio ?: savedAspectRatio,
                shapeId = savedShapeId ?: current.shapeId,
            )
        }
    }

    fun photoSelected(source: Uri?) {
        if (source == null) return

        _state.update { current ->
            current.copy(
                message = PhotoWidgetConfigureState.Message.LaunchCrop(
                    source = source,
                    destination = photoWidgetStorage.newWidgetPhoto(appWidgetId = appWidgetId)
                        .let(Uri::fromFile),
                    aspectRatio = current.aspectRatio,
                ),
            )
        }
    }

    fun photoCropped(path: String) {
        _state.update { current -> current.copy(photos = current.photos + path) }
    }

    fun photoRemoved(path: String) {
        photoWidgetStorage.deleteWidgetPhoto(path)
        _state.update { current -> current.copy(photos = current.photos - path) }
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
                            photoPath = currentState.photos.first(),
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
                    enableLooping = currentState.photos.size > 1,
                    loopingInterval = currentState.loopingInterval,
                    aspectRatio = currentState.aspectRatio,
                    shapeId = currentState.shapeId,
                )

                _state.update { current ->
                    current.copy(
                        message = PhotoWidgetConfigureState.Message.AddWidget(
                            appWidgetId = appWidgetId,
                            photoPath = currentState.photos.first(),
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
