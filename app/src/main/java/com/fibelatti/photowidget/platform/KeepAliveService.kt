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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class KeepAliveService : Service() {

    @Inject
    lateinit var gifPlaybackController: GifPlaybackController

    // Main.immediate starts collecting within onCreate, before any later command can be emitted.
    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

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

        collectGifCommands()
        registerScreenStateReceiver()

        logger.d("Service is running...")
    }

    override fun onDestroy() {
        logger.i("Destroying keep-alive service.")

        serviceScope.cancel()
        unregisterScreenStateReceiver()
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

    private fun collectGifCommands() {
        serviceScope.launch {
            gifCommands.collect { command: GifCommand ->
                logger.i("Gif command received %s", mapOf("command" to command))

                when (command) {
                    is GifCommand.Setup -> gifPlaybackController.setupWidgetGif(appWidgetId = command.appWidgetId)
                    is GifCommand.TearDown -> gifPlaybackController.tearDownWidgetGif(appWidgetId = command.appWidgetId)
                    is GifCommand.Resume -> gifPlaybackController.setPlaybackAllowed(true, command.appWidgetId)
                    is GifCommand.Pause -> gifPlaybackController.setPlaybackAllowed(false, command.appWidgetId)
                }
            }
        }
    }

    private fun registerScreenStateReceiver() {
        registerReceiver(
            /* receiver = */ screenStateBroadcastReceiver,
            /* filter = */ IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) },
        )
    }

    private fun unregisterScreenStateReceiver() {
        try {
            unregisterReceiver(screenStateBroadcastReceiver)
        } catch (_: Exception) {
            // An exception here only means that the receiver didn't get a chance to be registered
        }
    }

    private sealed interface GifCommand {

        data class Setup(val appWidgetId: Int) : GifCommand
        data class TearDown(val appWidgetId: Int) : GifCommand
        data class Resume(val appWidgetId: Int) : GifCommand
        data class Pause(val appWidgetId: Int) : GifCommand
    }

    companion object {

        private val logger: Timber.Tree = Timber.tag("KeepAliveService")

        private const val NOTIFICATION_CHANNEL_ID: String = "keep-alive-service"

        /**
         * Commands for the running service. They are dropped while the service isn't running, since
         * there is no replay for a new collector. A single flow keeps commands in the order they were
         * sent, and the unlimited buffer makes [MutableSharedFlow.tryEmit] always succeed, so that
         * senders aren't required to suspend.
         */
        private val gifCommands: MutableSharedFlow<GifCommand> = MutableSharedFlow(
            extraBufferCapacity = Channel.UNLIMITED,
        )

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

        fun setupGif(appWidgetId: Int) {
            gifCommands.tryEmit(GifCommand.Setup(appWidgetId = appWidgetId))
        }

        fun tearDownGif(appWidgetId: Int) {
            gifCommands.tryEmit(GifCommand.TearDown(appWidgetId = appWidgetId))
        }

        fun resumeGif(appWidgetId: Int) {
            gifCommands.tryEmit(GifCommand.Resume(appWidgetId = appWidgetId))
        }

        fun pauseGif(appWidgetId: Int) {
            gifCommands.tryEmit(GifCommand.Pause(appWidgetId = appWidgetId))
        }
    }
}
