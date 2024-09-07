package com.fibelatti.photowidget.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) : ViewModel() {

    private val _currentWidgets: MutableStateFlow<List<Pair<Int, PhotoWidget>>> = MutableStateFlow(emptyList())
    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = _currentWidgets.asStateFlow()

    fun loadCurrentWidgets(ids: List<Int>) {
        val allIds = ids + photoWidgetStorage.getPendingDeletionWidgetIds()
        val flows = allIds.map(loadPhotoWidgetUseCase::invoke)

        combine(flows, Array<PhotoWidget>::toList)
            .withIndex()
            .onEach { (emissionIndex, widgets) ->
                _currentWidgets.value = widgets.withIndex().mapNotNull { (index, widget) ->
                    if (emissionIndex > 0 && widget.photos.isEmpty()) return@mapNotNull null
                    allIds[index] to widget
                }
            }
            .launchIn(viewModelScope)
    }

    fun deleteWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.deleteWidgetData(appWidgetId = appWidgetId)
            _currentWidgets.update { current -> current.filter { it.first != appWidgetId } }
        }
    }
}
