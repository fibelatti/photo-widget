package com.fibelatti.photowidget.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.preferences.UserPreferences
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppTheme(
    appThemeViewModel: AppThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val userPreferences by appThemeViewModel.userPreferences.collectAsStateWithLifecycle()

    ExtendedTheme(
        dynamicColor = userPreferences.dynamicColors,
        content = content,
    )
}

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    userPreferencesStorage: UserPreferencesStorage,
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesStorage.userPreferences
}
