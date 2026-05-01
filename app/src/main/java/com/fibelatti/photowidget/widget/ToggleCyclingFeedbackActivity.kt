package com.fibelatti.photowidget.widget

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val context: Context = this
        val appWidgetId: Int = intent.appWidgetId

        lifecycleScope.launch {
            val previouslyPaused: Boolean? = withContext(Dispatchers.IO) {
                togglePaused(appWidgetId = appWidgetId)
            }

            if (previouslyPaused != null) {
                PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)

                val resId: Int = if (previouslyPaused) {
                    R.string.photo_widget_cycling_feedback_resumed
                } else {
                    R.string.photo_widget_cycling_feedback_paused
                }

                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }

    private suspend fun togglePaused(appWidgetId: Int): Boolean? {
        if (photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId)) return null

        val paused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)
        val source: PhotoWidgetSource = photoWidgetStorage.getWidgetSource(appWidgetId = appWidgetId)

        when {
            source == PhotoWidgetSource.GIF && paused -> {
                KeepAliveService.sendResumeGifBroadcast(context = this, appWidgetId = appWidgetId)
            }

            source == PhotoWidgetSource.GIF -> {
                KeepAliveService.sendPauseGifBroadcast(context = this, appWidgetId = appWidgetId)
            }

            paused -> {
                photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
            }

            else -> {
                photoWidgetAlarmManager.cancel(appWidgetId = appWidgetId)
            }
        }

        photoWidgetStorage.saveWidgetCyclePaused(appWidgetId = appWidgetId, value = !paused)

        return paused
    }
}
