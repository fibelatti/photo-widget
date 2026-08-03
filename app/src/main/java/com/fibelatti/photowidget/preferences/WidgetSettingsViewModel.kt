package com.fibelatti.photowidget.preferences

import android.content.Context
import androidx.lifecycle.ViewModel
import com.fibelatti.photowidget.widget.PhotoWidgetSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesStorage: UserPreferencesStorage,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesStorage.userPreferences

    fun saveEnableCrossfade(value: Boolean) {
        userPreferencesStorage.widgetEnableCrossfade = value
    }

    fun saveFolderSyncInterval(value: Int) {
        userPreferencesStorage.folderSyncInterval = value
        PhotoWidgetSyncWorker.enqueueWork(context = context)
    }
}
