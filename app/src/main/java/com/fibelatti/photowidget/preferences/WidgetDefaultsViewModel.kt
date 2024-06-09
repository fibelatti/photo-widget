package com.fibelatti.photowidget.preferences

import androidx.lifecycle.ViewModel
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class WidgetDefaultsViewModel @Inject constructor(
    private val userPreferencesStorage: UserPreferencesStorage,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesStorage.userPreferences

    fun saveDefaultSource(source: PhotoWidgetSource) {
        userPreferencesStorage.defaultSource = source
    }

    fun saveDefaultShuffle(value: Boolean) {
        userPreferencesStorage.defaultShuffle = value
    }

    fun saveDefaultIntervalEnabled(value: Boolean) {
        userPreferencesStorage.defaultIntervalEnabled = value
    }

    fun saveDefaultInterval(interval: PhotoWidgetLoopingInterval) {
        userPreferencesStorage.defaultInterval = interval
    }

    fun saveDefaultShape(value: String) {
        userPreferencesStorage.defaultShape = value
    }

    fun saveDefaultCornerRadius(value: Float) {
        userPreferencesStorage.defaultCornerRadius = value
    }

    fun saveDefaultOpacity(value: Float) {
        userPreferencesStorage.defaultOpacity = value
    }

    fun saveDefaultTapAction(tapAction: PhotoWidgetTapAction) {
        userPreferencesStorage.defaultTapAction = tapAction
    }

    fun saveDefaultIncreaseBrightness(value: Boolean) {
        userPreferencesStorage.defaultIncreaseBrightness = value
    }

    fun clearDefaults() {
        userPreferencesStorage.clearDefaults()
    }
}
