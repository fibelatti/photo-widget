package com.fibelatti.photowidget.hints

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HintStorage @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        "com.fibelatti.photowidget.HintPreferences",
        Context.MODE_PRIVATE,
    )

    var showFullScreenViewerHint: Boolean
        get() = sharedPreferences.getBoolean(Hint.FULL_SCREEN_VIEWER.value, true)
        set(value) {
            sharedPreferences.edit { putBoolean(Hint.FULL_SCREEN_VIEWER.value, value) }
        }

    private enum class Hint(val value: String) {
        FULL_SCREEN_VIEWER(value = "hint_full_screen_viewer"),
    }
}
