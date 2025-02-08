package com.fibelatti.photowidget

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import com.fibelatti.photowidget.platform.DynamicBorderReceiver
import com.fibelatti.photowidget.preferences.Appearance
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.DeleteStaleDataUseCase
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    @Inject
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var deleteStaleDataUseCase: DeleteStaleDataUseCase

    override fun onCreate() {
        super.onCreate()

        setupDebugMode()
        setupNightMode()
        setupDynamicColors()
        deleteStaleData()
        registerReceivers()
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

    private fun registerReceivers() {
        DynamicBorderReceiver.register(context = this)
    }
}
