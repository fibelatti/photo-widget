package com.fibelatti.photowidget.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
) : ViewModel() {

    private val _currentWidgets: MutableStateFlow<List<Pair<Int, PhotoWidget>>> = MutableStateFlow(emptyList())
    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = _currentWidgets.asStateFlow()

    fun loadCurrentWidgets(ids: List<Int>) {
        viewModelScope.launch {
            _currentWidgets.value = ids.map { id ->
                async { id to loadPhotoWidgetUseCase(appWidgetId = id) }
            }.awaitAll()
        }
    }
}
