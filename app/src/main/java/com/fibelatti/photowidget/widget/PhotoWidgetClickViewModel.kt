package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.savedState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoWidgetClickViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val flipPhotoUseCase: FlipPhotoUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) : ViewModel() {

    private val appWidgetId: Int by savedStateHandle.savedState(
        key = AppWidgetManager.EXTRA_APPWIDGET_ID,
        default = AppWidgetManager.INVALID_APPWIDGET_ID,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val photoWidget = async {
                loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true)
            }
            val count = async {
                photoWidgetStorage.getWidgetPhotoCount(appWidgetId = appWidgetId)
            }

            _state.update { current ->
                current.copy(
                    photoWidget = photoWidget.await(),
                    showMoveControls = count.await() > 1,
                )
            }
        }
    }

    fun flip(backwards: Boolean = false) {
        viewModelScope.launch {
            flipPhotoUseCase(appWidgetId = appWidgetId, flipBackwards = backwards)

            _state.update { current ->
                current.copy(
                    photoWidget = loadPhotoWidgetUseCase(appWidgetId = appWidgetId, currentPhotoOnly = true),
                )
            }
        }
    }

    data class State(
        val photoWidget: PhotoWidget? = null,
        val showMoveControls: Boolean = false,
    )
}
