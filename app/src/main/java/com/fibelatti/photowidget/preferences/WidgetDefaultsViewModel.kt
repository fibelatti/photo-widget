package com.fibelatti.photowidget.preferences

import androidx.lifecycle.ViewModel
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
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

    fun saveDefaultCycleMode(cycleMode: PhotoWidgetCycleMode) {
        userPreferencesStorage.defaultCycleMode = cycleMode
    }

    fun saveDefaultShape(value: String) {
        userPreferencesStorage.defaultShape = value
    }

    fun saveDefaultCornerRadius(value: Int) {
        userPreferencesStorage.defaultCornerRadius = value
    }

    fun saveDefaultOpacity(value: Float) {
        userPreferencesStorage.defaultOpacity = value
    }

    fun saveDefaultSaturation(value: Float) {
        userPreferencesStorage.defaultSaturation = value
    }

    fun saveDefaultBrightness(value: Float) {
        userPreferencesStorage.defaultBrightness = value
    }

    fun saveDefaultTapAction(tapAction: PhotoWidgetTapAction) {
        userPreferencesStorage.defaultTapAction = tapAction
    }

    fun clearDefaults() {
        userPreferencesStorage.clearDefaults()
    }
}
