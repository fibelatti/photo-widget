package com.fibelatti.photowidget.widget

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This could have been a `BroadcastReceiver` but the action would be quite confusing without any
 * sort of user feedback. This transparent activity does the work and shows a toast at the end to
 * confirm.
 */
@AndroidEntryPoint
class ToggleCyclingFeedbackActivity : AppCompatActivity() {

    @Inject
    lateinit var photoWidgetStorage: PhotoWidgetStorage

    @Inject
    lateinit var photoWidgetAlarmManager: PhotoWidgetAlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (photoWidgetStorage.getWidgetLockedInApp(appWidgetId = intent.appWidgetId)) {
            finish()
            return
        }

        val paused = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = intent.appWidgetId)

        if (paused) {
            photoWidgetAlarmManager.setup(appWidgetId = intent.appWidgetId)
        } else {
            photoWidgetAlarmManager.cancel(appWidgetId = intent.appWidgetId)
        }

        photoWidgetStorage.saveWidgetCyclePaused(appWidgetId = intent.appWidgetId, value = !paused)

        PhotoWidgetProvider.update(
            context = this,
            appWidgetId = intent.appWidgetId,
        )

        Toast.makeText(
            /* context = */
            this,
            /* resId = */
            if (paused) {
                R.string.photo_widget_cycling_feedback_resumed
            } else {
                R.string.photo_widget_cycling_feedback_paused
            },
            /* duration = */
            Toast.LENGTH_SHORT,
        ).show()

        finish()
    }
}
