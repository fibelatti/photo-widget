package com.fibelatti.photowidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fibelatti.photowidget.home.Appearance
import com.fibelatti.photowidget.home.UserPreferencesStorage
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    @Inject
    lateinit var photoWidgetStorage: PhotoWidgetStorage

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        setupNightMode()
        setupDynamicColors()
        deleteUnusedWidgetData()
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

    private fun deleteUnusedWidgetData() {
        photoWidgetStorage.deleteUnusedWidgetData(
            existingWidgetIds = PhotoWidgetProvider.ids(context = this),
        )
    }
}
