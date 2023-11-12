package com.fibelatti.photowidget.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.home.UserPreferences
import com.fibelatti.photowidget.home.UserPreferencesStorage
import com.fibelatti.ui.theme.ExtendedTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

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
