package com.fibelatti.photowidget

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.ComposeRuntimeFlags
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fibelatti.photowidget.platform.ConfigurationChangedReceiver
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.platform.FileLoggingTree
import com.fibelatti.photowidget.preferences.Appearance
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.DeleteStaleDataUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetRescheduleWorker
import com.fibelatti.photowidget.widget.PhotoWidgetSyncWorker
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    @Inject
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var deleteStaleDataUseCase: Lazy<DeleteStaleDataUseCase>

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var fileLoggingTree: FileLoggingTree

    @Inject
    lateinit var exceptionReporter: ExceptionReporter

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        setupLogging()
        setupExceptionHandler()
        setupDebugMode()
        setupNightMode()
        setupDynamicColors()
        deleteStaleData()
        getReadyToWork()
    }

    private fun setupLogging() {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else fileLoggingTree)
    }

    /**
     * Records the failure before the process goes down, then hands it to the handler that was
     * already installed so the crash still surfaces as it otherwise would.
     *
     * Uncaught exceptions from coroutines reach this handler too: the application scope carries no
     * `CoroutineExceptionHandler`, so the machinery falls back to the thread's handler.
     *
     * Platform crashes the app cannot act on are logged but not offered as a report.
     */
    private fun setupExceptionHandler() {
        val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // The report and the log entries are written on this thread: anything queued elsewhere
            // dies with the process. Guarded so a failure here still lets the crash through.
            runCatching {
                Timber.e(throwable, "Uncaught exception")
                if (!throwable.isAbandonedJobCleanupTimeout()) {
                    exceptionReporter.collectReportBlocking(throwable = throwable)
                }
                fileLoggingTree.flushPendingEntries()
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Whether this is the finalizer watchdog killing the process because `JobScheduler` did not
     * answer the abandoned-job check within its timeout.
     *
     * The platform runs that check when the parameters of a stopped job are garbage collected, and
     * `WorkManager` drops them on stop as the `JobService` contract allows. The blocking call only
     * times out when the system itself is unresponsive, typically while the device shuts down.
     */
    private fun Throwable.isAbandonedJobCleanupTimeout(): Boolean {
        return this is TimeoutException &&
            stackTrace.any { it.className == $$"android.app.job.JobParameters$JobCleanupCallback" }
    }

    @OptIn(ExperimentalComposeApi::class)
    private fun setupDebugMode() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )

        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)

        ComposeRuntimeFlags.isLinkBufferComposerEnabled = true
    }

    private fun setupNightMode() {
        val mode = when (userPreferencesStorage.appearance) {
            Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setupDynamicColors() {
        val dynamicColorsOptions = DynamicColorsOptions.Builder()
            .setThemeOverlay(R.style.AppTheme_Overlay)
            .setPrecondition { _, _ -> userPreferencesStorage.dynamicColors }
            .build()

        DynamicColors.applyToActivitiesIfAvailable(this, dynamicColorsOptions)
    }

    private fun deleteStaleData() {
        coroutineScope.launch {
            deleteStaleDataUseCase.get().invoke()
        }
    }

    private fun getReadyToWork() {
        ConfigurationChangedReceiver.register(context = this)

        PhotoWidgetRescheduleWorker.enqueueWork(context = this)
        PhotoWidgetSyncWorker.enqueueWork(context = this)
    }
}
