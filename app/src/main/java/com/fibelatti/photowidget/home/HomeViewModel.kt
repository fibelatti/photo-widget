package com.fibelatti.photowidget.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
    private val exceptionReporter: ExceptionReporter,
    private val scope: CoroutineScope,
) : ViewModel() {

    private val widgetIds: MutableStateFlow<List<Int>> = MutableStateFlow(emptyList())
    private val updateSignal: Channel<Unit> = Channel()

    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = loadWidgetsById()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _pendingReport: MutableStateFlow<String?> = MutableStateFlow(null)
    val pendingReport: StateFlow<String?> = _pendingReport.asStateFlow()

    fun loadWidgets() {
        viewModelScope.launch {
            widgetIds.update { photoWidgetStorage.getKnownWidgetIds() }
            updateSignal.send(Unit)
        }
    }

    private fun loadWidgetsById(): Flow<List<Pair<Int, PhotoWidget>>> {
        return combine(widgetIds, updateSignal.receiveAsFlow()) { ids: List<Int>, _: Unit -> ids }
            .flatMapLatest { allIds: List<Int> ->
                combine(
                    flows = allIds.map(loadPhotoWidgetUseCase::invoke),
                    transform = Array<PhotoWidget>::toList,
                )
            }
            .withIndex()
            .map { (emissionIndex: Int, widgets: List<PhotoWidget>) ->
                val providerIds: List<Int> = PhotoWidgetProvider.ids(context)

                widgets.withIndex().mapNotNull { (index: Int, widget: PhotoWidget) ->
                    val widgetId: Int = widgetIds.value.getOrNull(index) ?: return@mapNotNull null
                    val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = widgetId)

                    widgetId to widget.copy(
                        status = when {
                            (emissionIndex > 0 && widget.photos.isEmpty()) -> PhotoWidgetStatus.INVALID
                            widget.deletionTimestamp > 0L -> PhotoWidgetStatus.REMOVED
                            widgetId in providerIds && isLocked -> PhotoWidgetStatus.LOCKED
                            widgetId in providerIds -> PhotoWidgetStatus.ACTIVE
                            else -> PhotoWidgetStatus.KEPT
                        },
                    )
                }
            }
    }

    fun syncPhotos(appWidgetId: Int) {
        scope.launch {
            withContext(NonCancellable) {
                if (PhotoWidgetSource.DIRECTORY == photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId)) {
                    photoWidgetStorage.syncWidgetPhotos(appWidgetId = appWidgetId)
                }
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

    fun checkForPendingExceptionReports() {
        viewModelScope.launch {
            val crashReports = exceptionReporter.getPendingReports()
            if (crashReports.isNotEmpty()) {
                val reportText: String = withContext(Dispatchers.IO) {
                    crashReports.last().readText()
                }
                _pendingReport.update { reportText }
            }
        }
    }

    fun clearPendingExceptionReports() {
        viewModelScope.launch {
            exceptionReporter.clearPendingReports()
        }
    }
}
