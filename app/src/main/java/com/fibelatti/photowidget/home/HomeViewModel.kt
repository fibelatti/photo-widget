package com.fibelatti.photowidget.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetLoadStage
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.platform.FileLoggingTree
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.LoadPhotoWidgetUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetAlarmManager
import com.fibelatti.photowidget.widget.WidgetProviders
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
    private val widgetProviders: WidgetProviders,
    private val userPreferencesStorage: UserPreferencesStorage,
    private val exceptionReporter: ExceptionReporter,
    private val fileLoggingTree: FileLoggingTree,
    private val scope: CoroutineScope,
) : ViewModel() {

    private val updateSignal: Channel<Unit> = Channel()

    val currentWidgets: StateFlow<List<Pair<Int, PhotoWidget>>> = loadWidgetsById()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val highlightTransparentWidgets: StateFlow<Boolean> = userPreferencesStorage.userPreferences
        .map { it.highlightTransparentWidgets }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = userPreferencesStorage.highlightTransparentWidgets,
        )

    private val _pendingReport: MutableStateFlow<PendingReport?> = MutableStateFlow(null)
    val pendingReport: StateFlow<PendingReport?> = _pendingReport.asStateFlow()

    fun loadWidgets() {
        viewModelScope.launch {
            updateSignal.send(Unit)
        }
    }

    private fun loadWidgetsById(): Flow<List<Pair<Int, PhotoWidget>>> {
        return combine(
            photoWidgetStorage.getKnownWidgetIds(),
            photoWidgetStorage.getDraftWidgetIds(),
            updateSignal.receiveAsFlow(),
        ) { ids: List<Int>, draftIds: List<Int>, _: Unit -> ids + draftIds }
            .flatMapLatest { allIds: List<Int> ->
                if (allIds.isEmpty()) return@flatMapLatest flowOf(emptyMap())

                // Combining collects every flow to completion, so each one stops at the current photo
                val widgetFlows: List<Flow<PhotoWidget>> = allIds.map { id: Int ->
                    loadPhotoWidgetUseCase(appWidgetId = id).transformWhile { widget: PhotoWidget ->
                        emit(widget)
                        widget.loadStage < PhotoWidgetLoadStage.CURRENT_PHOTO
                    }
                }

                combine(flows = widgetFlows) { widgets ->
                    allIds.withIndex().associate { (index, id) -> id to widgets[index] }
                }
            }
            .map { widgets: Map<Int, PhotoWidget> ->
                val providerIds: List<Int> = widgetProviders.photoWidgetIds() + widgetProviders.transparentWidgetIds()
                val persistedUris: List<Uri> = photoWidgetStorage.getPersistedUriPermissions()

                widgets.mapValues { (widgetId: Int, widget: PhotoWidget) ->
                    val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = widgetId)
                    val isPaused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = widgetId)
                    val status: PhotoWidgetStatus = when {
                        PhotoWidget.isDraftWidgetId(widgetId) -> PhotoWidgetStatus.DRAFT

                        isInvalid(widgetId = widgetId, widget = widget, persistedUris = persistedUris) -> {
                            PhotoWidgetStatus.INVALID
                        }

                        widget.deletionTimestamp > 0L -> PhotoWidgetStatus.REMOVED

                        isLocked && widgetId in providerIds -> PhotoWidgetStatus.LOCKED

                        isPaused && widgetId in providerIds -> PhotoWidgetStatus.PAUSED

                        widgetId in providerIds -> PhotoWidgetStatus.ACTIVE

                        else -> PhotoWidgetStatus.KEPT
                    }

                    widget.copy(status = status)
                }.toList()
            }
            .flowOn(Dispatchers.Default)
    }

    private fun isInvalid(widgetId: Int, widget: PhotoWidget, persistedUris: List<Uri>): Boolean {
        if (widget.transparent || widget.loadStage < PhotoWidgetLoadStage.CURRENT_PHOTO) return false

        // Widgets only load their cached photos, which outlive access to the source
        return widget.currentPhoto == null ||
            !photoWidgetStorage.hasSourceAccess(appWidgetId = widgetId, persistedUris = persistedUris)
    }

    fun syncPhotos(appWidgetId: Int) {
        scope.launch {
            withContext(NonCancellable) {
                if (photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId) == PhotoWidgetSource.DIRECTORY) {
                    if (photoWidgetStorage.syncWidgetPhotos(appWidgetId = appWidgetId)) {
                        widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)
                    }
                }
            }
        }
    }

    fun lockWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetLockedInApp(appWidgetId = appWidgetId, value = true)
            photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
            widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)

            updateSignal.send(Unit)
        }
    }

    fun unlockWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetLockedInApp(appWidgetId = appWidgetId, value = false)

            // Locking a paused widget keeps it paused. Unlocking only restores the lock state, so
            // the widget surfaces as paused and stays that way until it is intentionally resumed.
            if (!photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)) {
                photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
            }

            widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)

            updateSignal.send(Unit)
        }
    }

    fun resumeWidget(appWidgetId: Int) {
        viewModelScope.launch {
            photoWidgetStorage.saveWidgetCyclePaused(appWidgetId = appWidgetId, value = false)

            if (photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId) != PhotoWidgetSource.GIF) {
                photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
            }

            widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)

            updateSignal.send(Unit)
        }
    }

    fun setHighlightTransparentWidgets(value: Boolean) {
        viewModelScope.launch {
            userPreferencesStorage.highlightTransparentWidgets = value
            widgetProviders.transparentWidgetIds().forEach { id ->
                widgetProviders.updateTransparentWidget(appWidgetId = id)
            }
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
        }
    }

    fun checkForPendingExceptionReports() {
        viewModelScope.launch {
            val crashReports = exceptionReporter.getPendingReports()
            if (crashReports.isNotEmpty()) {
                val reportText: String = withContext(Dispatchers.IO) {
                    crashReports.last().readText()
                }
                val logFiles: List<File> = fileLoggingTree.getLogFiles()
                _pendingReport.update { PendingReport(text = reportText, logFiles = logFiles) }
            }
        }
    }

    fun clearPendingExceptionReports() {
        viewModelScope.launch {
            exceptionReporter.clearPendingReports()
        }
    }
}

/**
 * A crash report ready to be sent, together with the log files covering the period that led to it.
 */
data class PendingReport(
    val text: String,
    val logFiles: List<File>,
)
