package com.fibelatti.photowidget.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
    private val scope: CoroutineScope,
) : ViewModel() {

    private val widgetIds = MutableStateFlow(emptyList<Int>())
    private val updateSignal = Channel<Unit>()

    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = widgetIds
        .combine(updateSignal.receiveAsFlow()) { ids, _ -> ids }
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
            val providerIds = PhotoWidgetProvider.ids(context)

            widgets.withIndex().mapNotNull { (index, widget) ->
                if (emissionIndex > 0 && widget.photos.isEmpty()) return@mapNotNull null
                val widgetId = widgetIds.value.getOrNull(index) ?: return@mapNotNull null
                val isLocked = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = widgetId)

                widgetId to widget.copy(
                    status = when {
                        widget.deletionTimestamp > 0L -> PhotoWidgetStatus.REMOVED
                        widgetId in providerIds && isLocked -> PhotoWidgetStatus.LOCKED
                        widgetId in providerIds -> PhotoWidgetStatus.ACTIVE
                        else -> PhotoWidgetStatus.KEPT
                    },
                )
            }
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), initialValue = emptyList())

    fun loadCurrentWidgets() {
        viewModelScope.launch {
            widgetIds.update { photoWidgetStorage.getKnownWidgetIds() }
            updateSignal.send(Unit)
        }
    }

    fun syncPhotos(appWidgetId: Int) {
        scope.launch(NonCancellable) {
            if (PhotoWidgetSource.DIRECTORY == photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId)) {
                photoWidgetStorage.syncWidgetPhotos(appWidgetId = appWidgetId)
            }
        }
    }

    fun lockWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetLockedInApp(appWidgetId = appWidgetId, value = true)
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)

            updateSignal.send(Unit)
        }
    }

    fun unlockWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetLockedInApp(appWidgetId = appWidgetId, value = false)
            photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)

            updateSignal.send(Unit)
        }
    }

    fun keepWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = null)
            updateSignal.send(Unit)
        }
    }

    fun deleteWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.deleteWidgetData(appWidgetId = appWidgetId)
            widgetIds.update { current -> current - appWidgetId }
        }
    }
}
