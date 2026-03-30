package com.fibelatti.photowidget.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.R
import timber.log.Timber

class KeepAliveService : Service() {

    private val screenStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            logger.i("Broadcast received (action=${intent.action})")

            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    logger.d("Screen on: put widgets to work.")
                }

                Intent.ACTION_SCREEN_OFF -> {
                    logger.d("Screen off: put widgets to sleep.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger.i("Creating keep-alive service.")

        if (!startForeground()) {
            stopSelf()
            return
        }

        registerReceiver(
            screenStateBroadcastReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) },
        )

        logger.d("Service is running...")
    }

    override fun onDestroy() {
        logger.i("Destroying keep-alive service.")

        try {
            unregisterReceiver(screenStateBroadcastReceiver)
        } catch (_: Exception) {
            // An exception here only means that the receiver didn't get a chance to be registered
        }

        super.onDestroy()
    }

    private fun startForeground(): Boolean {
        try {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ NOTIFICATION_CHANNEL_ID.hashCode(),
                /* notification = */ createNotification(),
                /* foregroundServiceType = */ serviceType,
            )
        } catch (e: Exception) {
            logger.e(e, "Unable to start service as a foreground service.")
            return false
        }

        return true
    }

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

        return Notification.Builder(/* context = */ applicationContext, /* channelId = */ NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_channel_name_keep_alive))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent): IBinder? = null

    companion object {

        private val logger: Timber.Tree = Timber.tag("KeepAliveService")

        private const val NOTIFICATION_CHANNEL_ID: String = "keep-alive-service"

        private fun newIntent(context: Context): Intent {
            return Intent(context, KeepAliveService::class.java)
        }

        fun tryStart(context: Context) {
            val context: Context = context.applicationContext

            try {
                logger.i("Attempting to start the service.")
                context.startService(newIntent(context))
            } catch (e: Exception) {
                logger.e(e, "Unable to start service.")
            }
        }

        fun stop(context: Context) {
            context.stopService(newIntent(context))
        }
    }
}
