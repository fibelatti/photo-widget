package com.fibelatti.photowidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.WorkManager
import com.fibelatti.photowidget.home.Appearance
import com.fibelatti.photowidget.home.UserPreferencesStorage
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.photowidget.widget.PhotoWidgetStorage
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    @Inject
    lateinit var photoWidgetStorage: PhotoWidgetStorage

    override fun onCreate() {
        super.onCreate()

        setupNightMode()
        setupDynamicColors()

        val widgetIds = PhotoWidgetProvider.ids(context = this).ifEmpty { return }
        deleteUnusedWidgetData(widgetIds)
        cancelLegacyWork(widgetIds)
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

    private fun deleteUnusedWidgetData(widgetIds: List<Int>) {
        photoWidgetStorage.deleteUnusedWidgetData(existingWidgetIds = widgetIds)
    }

    /**
     * Flipping widgets used to be controlled by [WorkManager] but have been migrated to `AlarmManager` instead
     * to support shorter intervals. This function cancels any work that was scheduled before the update.
     */
    private fun cancelLegacyWork(widgetIds: List<Int>) {
        val workManager = WorkManager.getInstance(this)
        for (id in widgetIds) {
            workManager.cancelUniqueWork("LoopingPhotoWidgetWorker_$id")
        }
    }
}
