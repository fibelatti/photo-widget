package com.fibelatti.photowidget.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.R
import timber.log.Timber

class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        Timber.d("Creating keep-alive service.")

        val notification = createNotification()

        try {
            startForeground(NOTIFICATION_CHANNEL_ID.hashCode(), notification)
        } catch (e: Exception) {
            // This can run as the result of a device boot, in which case `startService` will throw due to
            // background restrictions. In such case, the service will be started thanks to `RecurringWorker`
            Timber.e(e)
        }

        ConfigurationChangedReceiver.register(context = this)
    }

    override fun onDestroy() {
        Timber.d("Destroying keep-alive service.")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun createNotification(): Notification {
        val notificationManager: NotificationManager? = getSystemService()

        val notificationChannel: NotificationChannel = NotificationChannel(
            /* id = */ NOTIFICATION_CHANNEL_ID,
            /* name = */ getString(R.string.notification_channel_name_keep_alive),
            /* importance = */ NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description_keep_alive)
            setShowBadge(false)
        }

        notificationManager?.createNotificationChannel(notificationChannel)

        return Notification.Builder(
            /* context = */ applicationContext,
            /* channelId = */ NOTIFICATION_CHANNEL_ID,
        ).apply {
            setContentTitle(getString(R.string.notification_channel_name_keep_alive))
            setOngoing(true)
        }.build()
    }

    companion object {

        private const val NOTIFICATION_CHANNEL_ID: String = "keep-alive-service"

        fun tryStart(context: Context) {
            try {
                context.startService(Intent(context, KeepAliveService::class.java))
            } catch (e: Exception) {
                // The service cannot be started in response to `BOOT_COMPLETED` because of its type.
                // If the app isn't open to start it "manually", `RecurringWorker` will start it.
                Timber.e(e)
            }
        }
    }
}
