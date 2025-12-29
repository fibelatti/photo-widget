package com.fibelatti.photowidget.help

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HintStorage @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        "com.fibelatti.photowidget.HintPreferences",
        Context.MODE_PRIVATE,
    )

    var showHomeBackgroundRestrictionsHint: Boolean
        get() = sharedPreferences.getBoolean(Hint.HOME_BACKGROUND_RESTRICTIONS.value, true)
        set(value) {
            sharedPreferences.edit { putBoolean(Hint.HOME_BACKGROUND_RESTRICTIONS.value, value) }
        }

    private enum class Hint(val value: String) {
        HOME_BACKGROUND_RESTRICTIONS(value = "hint_home_background_restrictions"),
    }
}
