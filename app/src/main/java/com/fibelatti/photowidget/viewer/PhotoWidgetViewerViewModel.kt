package com.fibelatti.photowidget.viewer

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.tapActionKeepCurrentPhoto
import com.fibelatti.photowidget.model.tapActionNoShuffle
import com.fibelatti.photowidget.platform.savedState
import com.fibelatti.photowidget.widget.CyclePhotoUseCase
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
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
    private val photoWidgetStorage: PhotoWidgetStorage,
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
            val photoWidget = _state.value.photoWidget
            val newPhotoId = cyclePhotoUseCase(
                appWidgetId = appWidgetId,
                direction = direction,
                noShuffle = photoWidget.tapActionNoShuffle,
                skipSaving = photoWidget.tapActionKeepCurrentPhoto,
                currentPhoto = photoWidget.currentPhoto?.photoId,
            )
            // Read by ID rather than reloading the current photo: when the tap action keeps the
            // current photo, cycling doesn't save the new one
            val newPhoto: LocalPhoto? = newPhotoId.ifEmpty { null }?.let { photoId: String ->
                photoWidgetStorage.getWidgetPhoto(appWidgetId = appWidgetId, photoId = photoId)
            }

            updateState(fromWidget = newPhoto?.let { photoWidget.copy(currentPhoto = it) })
        }
    }

    private fun updateState(fromWidget: PhotoWidget? = null) {
        loadWidgetJob?.cancel()
        loadWidgetJob = viewModelScope.launch {
            val photoWidget: PhotoWidget = fromWidget
                ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true).first { !it.isLoading }
            val currentPhotoId: String? = photoWidget.currentPhoto?.photoId
            // The adjacent photo is exact, the count is the fallback when the current photo can't be placed
            val canCycle: Boolean = currentPhotoId
                ?.let { photoId: String ->
                    photoWidgetStorage.getAdjacentWidgetPhotoId(
                        appWidgetId = appWidgetId,
                        photoId = photoId,
                        forward = true,
                    ) != currentPhotoId
                }
                ?: (photoWidgetStorage.getWidgetPhotoCountEstimate(appWidgetId = appWidgetId) > 1)

            _state.update { current ->
                current.copy(
                    photoWidget = photoWidget,
                    showNextButton = canCycle,
                    showPreviousButton = canCycle,
                )
            }
        }
    }

    data class State(
        val photoWidget: PhotoWidget = PhotoWidget(),
        val showNextButton: Boolean = false,
        val showPreviousButton: Boolean = false,
    )
}
