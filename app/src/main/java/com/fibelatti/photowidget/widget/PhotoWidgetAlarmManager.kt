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
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
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
    private val canScheduleExactAlarms: Boolean
        get() = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
            .also { Timber.d("Schedule exact alarms permission granted: $it") }

    fun setup(appWidgetId: Int) {
        Timber.d("Setting alarm for widget (appWidgetId=$appWidgetId)")

        when (val cycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = appWidgetId)) {
            is PhotoWidgetCycleMode.Interval -> setupIntervalAlarm(cycleMode = cycleMode, appWidgetId = appWidgetId)
            is PhotoWidgetCycleMode.Schedule -> setupScheduleAlarm(cycleMode = cycleMode, appWidgetId = appWidgetId)
            is PhotoWidgetCycleMode.Disabled -> return
        }
    }

    fun cancel(appWidgetId: Int) {
        Timber.d("Cancelling alarm for widget (appWidgetId=$appWidgetId)")

        alarmManager.cancel(PhotoWidgetProvider.flipPhotoPendingIntent(context = context, appWidgetId = appWidgetId))
        alarmManager.cancel(ExactRepeatingAlarmReceiver.pendingIntent(context = context, appWidgetId = appWidgetId))
    }

    private fun setupIntervalAlarm(cycleMode: PhotoWidgetCycleMode.Interval, appWidgetId: Int) {
        val intervalMillis = cycleMode.loopingInterval.run { timeUnit.toMillis(repeatInterval) }
        val nextCycleTime = photoWidgetStorage.getWidgetNextCycleTime(appWidgetId = appWidgetId)
        val currentTimeMillis = System.currentTimeMillis()
        val triggerAtMillis = if (nextCycleTime > currentTimeMillis) {
            nextCycleTime
        } else {
            currentTimeMillis + intervalMillis
        }

        photoWidgetStorage.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = triggerAtMillis)

        if (canScheduleExactAlarms) {
            try {
                alarmManager.setExact(
                    /* type = */ AlarmManager.RTC,
                    /* triggerAtMillis = */ triggerAtMillis,
                    /* operation = */
                    ExactRepeatingAlarmReceiver.pendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                    ),
                )
            } catch (_: SecurityException) {
                Timber.d("SecurityException: fallback to inexact alarm")

                setRepeatingAlarm(
                    triggerAtMillis = triggerAtMillis,
                    intervalMillis = intervalMillis,
                    appWidgetId = appWidgetId,
                )
            }
        } else {
            setRepeatingAlarm(
                triggerAtMillis = triggerAtMillis,
                intervalMillis = intervalMillis,
                appWidgetId = appWidgetId,
            )
        }
    }

    private fun setRepeatingAlarm(triggerAtMillis: Long, intervalMillis: Long, appWidgetId: Int) {
        alarmManager.setRepeating(
            /* type = */ AlarmManager.RTC,
            /* triggerAtMillis = */ triggerAtMillis,
            /* intervalMillis = */ intervalMillis,
            /* operation = */ PhotoWidgetProvider.flipPhotoPendingIntent(context = context, appWidgetId = appWidgetId),
        )
    }

    private fun setupScheduleAlarm(cycleMode: PhotoWidgetCycleMode.Schedule, appWidgetId: Int) {
        val calendar = Calendar.getInstance()

        val nextSameDayTrigger = cycleMode.triggers.firstOrNull { (hour, minute) ->
            hour >= calendar.get(Calendar.HOUR_OF_DAY) && minute > calendar.get(Calendar.MINUTE)
        }

        calendar.apply {
            if (nextSameDayTrigger != null) {
                set(Calendar.HOUR_OF_DAY, nextSameDayTrigger.hour)
                set(Calendar.MINUTE, nextSameDayTrigger.minute)
            } else {
                val nextTrigger = cycleMode.triggers.first()

                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, nextTrigger.hour)
                set(Calendar.MINUTE, nextTrigger.minute)
            }

            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (canScheduleExactAlarms) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    /* type = */ AlarmManager.RTC,
                    /* triggerAtMillis = */ calendar.timeInMillis,
                    /* operation = */
                    ExactRepeatingAlarmReceiver.pendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                    ),
                )
            } catch (_: SecurityException) {
                Timber.d("SecurityException: fallback to inexact alarm")
                setAlarm(triggerAtMillis = calendar.timeInMillis, appWidgetId = appWidgetId)
            }
        } else {
            setAlarm(triggerAtMillis = calendar.timeInMillis, appWidgetId = appWidgetId)
        }
    }

    private fun setAlarm(triggerAtMillis: Long, appWidgetId: Int) {
        alarmManager.setAndAllowWhileIdle(
            /* type = */ AlarmManager.RTC,
            /* triggerAtMillis = */ triggerAtMillis,
            /* operation = */ ExactRepeatingAlarmReceiver.pendingIntent(context = context, appWidgetId = appWidgetId),
        )
    }
}

class ExactRepeatingAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        entryPoint<PhotoWidgetEntryPoint>(context).runCatching {
            coroutineScope().launch {
                flipPhotoUseCase().invoke(appWidgetId = intent.appWidgetId)
                photoWidgetStorage().saveWidgetNextCycleTime(appWidgetId = intent.appWidgetId, nextCycleTime = -1)
                photoWidgetAlarmManager().setup(appWidgetId = intent.appWidgetId)
            }
        }
    }

    companion object {

        fun pendingIntent(
            context: Context,
            appWidgetId: Int,
        ): PendingIntent {
            val intent = Intent(context, ExactRepeatingAlarmReceiver::class.java).apply {
                setIdentifierCompat("$appWidgetId")
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
