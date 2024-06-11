package com.fibelatti.photowidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class PhotoWidgetAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    private val alarmManager: AlarmManager by lazy { requireNotNull(context.getSystemService()) }

    fun setup(appWidgetId: Int) {
        Timber.d("Setting alarm for widget (appWidgetId=$appWidgetId)")

        val widgetInterval = photoWidgetStorage.getWidgetInterval(appWidgetId = appWidgetId)
        val intervalMillis = widgetInterval.timeUnit.toMillis(widgetInterval.repeatInterval)

        if (AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
            try {
                Timber.d("Permission was granted, scheduling exact alarm")

                alarmManager.setExact(
                    /* type = */ AlarmManager.RTC,
                    /* triggerAtMillis = */ System.currentTimeMillis() + intervalMillis,
                    /* operation = */ ExactRepeatingAlarmReceiver.pendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                    ),
                )
            } catch (_: SecurityException) {
                Timber.d("SecurityException: fallback to inexact alarm")

                setRepeatingAlarm(intervalMillis = intervalMillis, appWidgetId = appWidgetId)
            }
        } else {
            Timber.d("Permission was not granted, scheduling inexact alarm")

            setRepeatingAlarm(intervalMillis = intervalMillis, appWidgetId = appWidgetId)
        }
    }

    private fun setRepeatingAlarm(intervalMillis: Long, appWidgetId: Int) {
        alarmManager.setRepeating(
            /* type = */ AlarmManager.RTC,
            /* triggerAtMillis = */ System.currentTimeMillis() + intervalMillis,
            /* intervalMillis = */ intervalMillis,
            /* operation = */ PhotoWidgetProvider.flipPhotoPendingIntent(context = context, appWidgetId = appWidgetId),
        )
    }

    fun cancel(appWidgetId: Int) {
        Timber.d("Cancelling alarm for widget (appWidgetId=$appWidgetId)")

        alarmManager.cancel(PhotoWidgetProvider.flipPhotoPendingIntent(context = context, appWidgetId = appWidgetId))
        alarmManager.cancel(ExactRepeatingAlarmReceiver.pendingIntent(context = context, appWidgetId = appWidgetId))
    }
}

class ExactRepeatingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)

            entryPoint.coroutineScope().launch {
                entryPoint.flipPhotoUseCase().invoke(appWidgetId = intent.appWidgetId)
            }

            entryPoint.photoWidgetAlarmManager().setup(appWidgetId = intent.appWidgetId)
        }
    }

    companion object {

        fun pendingIntent(
            context: Context,
            appWidgetId: Int,
        ): PendingIntent {
            val intent = Intent(context, ExactRepeatingAlarmReceiver::class.java).apply {
                this.appWidgetId = appWidgetId
            }
            return PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
