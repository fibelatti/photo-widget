package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.setIdentifierCompat
import timber.log.Timber

/**
 * Controls whichever app currently owns the media session by sending it media key events, the same
 * events a headset button or a keyboard media key would send.
 */
class MediaKeyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action: Action? = Action.fromValue(intent.action)

        Timber.i("Working... %s", mapOf("action" to action))

        if (action == null) return

        val audioManager: AudioManager = context.getSystemService() ?: return
        val downTime: Long = SystemClock.uptimeMillis()

        val feedback = when (action) {
            Action.PLAY_PAUSE -> R.string.photo_widget_configure_tap_action_media_play_pause
            Action.NEXT_TRACK -> R.string.photo_widget_configure_tap_action_media_next_track
            Action.PREVIOUS_TRACK -> R.string.photo_widget_configure_tap_action_media_previous_track
        }

        context.startActivity(
            HeadlessFeedbackActivity.newIntent(
                context = context,
                message = context.getString(feedback),
            ),
        )

        // Both events are required: apps that implement double tap to skip track measure the
        // interval between them, so a key press without a matching release is either ignored or
        // misread as a long press.
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, action.keyCode, 0),
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, action.keyCode, 0),
        )
    }

    enum class Action(val value: String, val keyCode: Int) {

        PLAY_PAUSE(value = "ACTION_MEDIA_PLAY_PAUSE", keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
        NEXT_TRACK(value = "ACTION_MEDIA_NEXT_TRACK", keyCode = KeyEvent.KEYCODE_MEDIA_NEXT),
        PREVIOUS_TRACK(value = "ACTION_MEDIA_PREVIOUS_TRACK", keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS),
        ;

        companion object {

            fun fromValue(value: String?): Action? = entries.firstOrNull { it.value == value }
        }
    }

    companion object {

        fun pendingIntent(context: Context, appWidgetId: Int, action: Action): PendingIntent {
            val intent = Intent(context, MediaKeyReceiver::class.java).apply {
                setIdentifierCompat("$appWidgetId")
                this.action = action.value
            }
            return PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
