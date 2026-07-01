package com.fibelatti.photowidget.preferences

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val userPreferencesStorage: UserPreferencesStorage,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesStorage.userPreferences

    fun saveEnableCrossfade(value: Boolean) {
        userPreferencesStorage.widgetEnableCrossfade = value
    }
}
