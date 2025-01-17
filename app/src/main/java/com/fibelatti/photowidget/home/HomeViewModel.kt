package com.fibelatti.photowidget.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
) : ViewModel() {

    private val widgetIds = MutableStateFlow(emptyList<Int>())

    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = widgetIds
        .flatMapLatest { allIds ->
            val flows = allIds.map(loadPhotoWidgetUseCase::invoke)
            if (flows.isNotEmpty()) {
                combine(flows, Array<PhotoWidget>::toList)
            } else {
                flowOf(emptyList())
            }
        }
        .withIndex()
        .map { (emissionIndex, widgets) ->
            widgets.withIndex().mapNotNull { (index, widget) ->
                if (emissionIndex > 0 && widget.photos.isEmpty()) return@mapNotNull null
                val widgetId = widgetIds.value.getOrNull(index) ?: return@mapNotNull null
                widgetId to widget
            }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

    fun loadCurrentWidgets() {
        widgetIds.update { photoWidgetStorage.getKnownWidgetIds() }
    }

    fun deleteWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.deleteWidgetData(appWidgetId = appWidgetId)
            widgetIds.update { current -> current - appWidgetId }
        }
    }
}
