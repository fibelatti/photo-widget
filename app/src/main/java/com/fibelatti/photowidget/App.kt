package com.fibelatti.photowidget

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fibelatti.photowidget.platform.ConfigurationChangedReceiver
import com.fibelatti.photowidget.preferences.Appearance
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.DeleteStaleDataUseCase
import com.fibelatti.photowidget.widget.PhotoWidgetRescheduleWorker
import com.fibelatti.photowidget.widget.PhotoWidgetSyncWorker
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
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
    lateinit var deleteStaleDataUseCase: DeleteStaleDataUseCase

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        setupDebugMode()
        setupNightMode()
        setupDynamicColors()
        deleteStaleData()
        getReadyToWork()
    }

    private fun setupDebugMode() {
        if (!BuildConfig.DEBUG) return

        Timber.plant(Timber.DebugTree())

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
            deleteStaleDataUseCase()
        }
    }

    private fun getReadyToWork() {
        ConfigurationChangedReceiver.register(context = this)

        PhotoWidgetRescheduleWorker.enqueueWork(context = this)
        PhotoWidgetSyncWorker.enqueueWork(context = this)
    }
}
