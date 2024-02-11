package com.fibelatti.photowidget.home

import android.content.Context
import androidx.core.content.edit
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesStorage @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val _userPreferences = MutableStateFlow(
        UserPreferences(
            appearance = appearance,
            dynamicColors = dynamicColors,
        ),
    )
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    var appearance: Appearance
        get() {
            val name = sharedPreferences.getString(PREFERENCES_KEY_APPEARANCE, null)

            return enumValueOfOrNull<Appearance>(name) ?: Appearance.FOLLOW_SYSTEM
        }
        set(value) {
            sharedPreferences.edit { putString(PREFERENCES_KEY_APPEARANCE, value.name) }
            _userPreferences.update { current -> current.copy(appearance = value) }
        }

    var dynamicColors: Boolean
        get() {
            return sharedPreferences.getBoolean(PREFERENCES_KEY_DYNAMIC_COLORS, true)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(PREFERENCES_KEY_DYNAMIC_COLORS, value) }
            _userPreferences.update { current -> current.copy(dynamicColors = value) }
        }

    private companion object {
        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.UserPreferences"

        const val PREFERENCES_KEY_APPEARANCE = "user_preferences_appearance"
        const val PREFERENCES_KEY_DYNAMIC_COLORS = "user_preferences_dynamic_colors"
    }
}
