package com.fibelatti.photowidget.viewer

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.tapActionKeepCurrentPhoto
import com.fibelatti.photowidget.model.tapActionNoShuffle
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.CyclePhotoUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoWidgetViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val cyclePhotoUseCase: CyclePhotoUseCase,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loadWidgetJob: Job? = null

    fun loadData() {
        updateState()
    }

    fun viewNextPhoto() {
        cycleWidget(direction = CyclePhotoUseCase.Direction.NEXT)
    }

    fun viewPreviousPhoto() {
        cycleWidget(direction = CyclePhotoUseCase.Direction.PREVIOUS)
    }

    private fun cycleWidget(direction: CyclePhotoUseCase.Direction) {
        viewModelScope.launch {
            val photoWidget = _state.value.photoWidget ?: return@launch
            val newPhotoId = cyclePhotoUseCase(
                appWidgetId = appWidgetId,
                direction = direction,
                noShuffle = photoWidget.tapActionNoShuffle,
                skipSaving = photoWidget.tapActionKeepCurrentPhoto,
                currentPhoto = photoWidget.currentPhoto?.photoId,
            )
            updateState(
                fromWidget = photoWidget.photos.firstOrNull { it.photoId == newPhotoId }
                    ?.let { newPhoto -> photoWidget.copy(currentPhoto = newPhoto) },
            )
        }
    }

    private fun updateState(fromWidget: PhotoWidget? = null) {
        loadWidgetJob?.cancel()
        loadWidgetJob = viewModelScope.launch {
            val photoWidget = fromWidget
                ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId).first { !it.isLoading }

            _state.update { current ->
                current.copy(
                    photoWidget = photoWidget,
                    showMoveControls = photoWidget.photos.size > 1,
                )
            }
        }
    }

    data class State(
        val photoWidget: PhotoWidget? = null,
        val showMoveControls: Boolean = false,
    )
}
