package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.hints.HintStorage
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.savedState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoWidgetClickViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val cyclePhotoUseCase: CyclePhotoUseCase,
    private val hintStorage: HintStorage,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loadWidgetJob: Job? = null

    init {
        loadWidgetJob = loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
            .onEach(::updateState)
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        hintStorage.showFullScreenViewerHint = false
        super.onCleared()
    }

    fun flip(backwards: Boolean = false) {
        loadWidgetJob?.cancel()
        loadWidgetJob = viewModelScope.launch {
            cyclePhotoUseCase(appWidgetId = appWidgetId, flipBackwards = backwards)

            loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                .onEach(::updateState)
                .launchIn(this)
        }
    }

    private fun updateState(photoWidget: PhotoWidget) {
        _state.update { current ->
            current.copy(
                photoWidget = photoWidget,
                showMoveControls = photoWidget.photos.size > 1,
                showHint = hintStorage.showFullScreenViewerHint,
            )
        }
    }

    data class State(
        val photoWidget: PhotoWidget? = null,
        val showMoveControls: Boolean = false,
        val showHint: Boolean = true,
    )
}
