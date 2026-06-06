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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class KeepAliveService : Service() {

    @Inject
    lateinit var gifPlaybackController: GifPlaybackController

    private val setupGifBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            logger.i(
                "SetupGifBroadcastReceiver: Broadcast received %s",
                mapOf("action" to intent.action, "appWidgetId" to intent.appWidgetId),
            )

            when (intent.action) {
                ACTION_SETUP_GIF -> gifPlaybackController.setupWidgetGif(appWidgetId = intent.appWidgetId)
                ACTION_TEAR_DOWN_GIF -> gifPlaybackController.tearDownWidgetGif(appWidgetId = intent.appWidgetId)
            }
        }
    }

    private val toggleBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            logger.i("ToggleBroadcastReceiver: Broadcast received %s", mapOf("action" to intent.action))

            when (intent.action) {
                ACTION_RESUME_GIF -> gifPlaybackController.setPlaybackAllowed(true, intent.appWidgetId)
                ACTION_PAUSE_GIF -> gifPlaybackController.setPlaybackAllowed(false, intent.appWidgetId)
            }
        }
    }

    private val screenStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            logger.i("ScreenStateBroadcastReceiver: Broadcast received %s", mapOf("action" to intent.action))

            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> gifPlaybackController.setPlaybackAllowed(true)
                Intent.ACTION_SCREEN_OFF -> gifPlaybackController.setPlaybackAllowed(false)
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

        registerReceivers()

        logger.d("Service is running...")
    }

    override fun onDestroy() {
        logger.i("Destroying keep-alive service.")

        unregisterReceivers()
        gifPlaybackController.tearDown()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent): IBinder? = null

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

    private fun registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
            /* receiver = */ setupGifBroadcastReceiver,
            /* filter = */ IntentFilter(ACTION_SETUP_GIF).apply { addAction(ACTION_TEAR_DOWN_GIF) },
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            /* receiver = */ toggleBroadcastReceiver,
            /* filter = */ IntentFilter(ACTION_RESUME_GIF).apply { addAction(ACTION_PAUSE_GIF) },
        )

        registerReceiver(
            /* receiver = */ screenStateBroadcastReceiver,
            /* filter = */ IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) },
        )
    }

    private fun unregisterReceivers() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(setupGifBroadcastReceiver)
        } catch (_: Exception) {
            // An exception here only means that the receiver didn't get a chance to be registered
        }

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(toggleBroadcastReceiver)
        } catch (_: Exception) {
            // An exception here only means that the receiver didn't get a chance to be registered
        }

        try {
            unregisterReceiver(screenStateBroadcastReceiver)
        } catch (_: Exception) {
            // An exception here only means that the receiver didn't get a chance to be registered
        }
    }

    companion object {

        private val logger: Timber.Tree = Timber.tag("KeepAliveService")

        private const val NOTIFICATION_CHANNEL_ID: String = "keep-alive-service"

        private const val ACTION_SETUP_GIF = "ACTION_SETUP_GIF"
        private const val ACTION_TEAR_DOWN_GIF = "ACTION_TEAR_DOWN_GIF"
        private const val ACTION_RESUME_GIF = "ACTION_RESUME_GIF"
        private const val ACTION_PAUSE_GIF = "ACTION_PAUSE_GIF"

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

        fun sendSetupGifBroadcast(context: Context, appWidgetId: Int) {
            val intent: Intent = Intent(ACTION_SETUP_GIF).apply {
                this.appWidgetId = appWidgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun sendTearDownGifBroadcast(context: Context, appWidgetId: Int) {
            val intent: Intent = Intent(ACTION_TEAR_DOWN_GIF).apply {
                this.appWidgetId = appWidgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun sendResumeGifBroadcast(context: Context, appWidgetId: Int) {
            val intent: Intent = Intent(ACTION_RESUME_GIF).apply {
                this.appWidgetId = appWidgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun sendPauseGifBroadcast(context: Context, appWidgetId: Int) {
            val intent: Intent = Intent(ACTION_PAUSE_GIF).apply {
                this.appWidgetId = appWidgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
