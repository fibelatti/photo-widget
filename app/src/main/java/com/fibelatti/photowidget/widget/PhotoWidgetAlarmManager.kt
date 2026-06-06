package com.fibelatti.photowidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.platform.EntryPointBroadcastReceiver
import com.fibelatti.photowidget.platform.intentExtras
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
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

    suspend fun setup(appWidgetId: Int) {
        Timber.i("Setting alarm for widget %s", mapOf("appWidgetId" to appWidgetId))

        if (photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId)) {
            Timber.d("Widget locked in-app. Skipping alarm setup.")
            return
        }

        cancel(appWidgetId)

        val cycleMode: PhotoWidgetCycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = appWidgetId)

        Timber.d("Widget alarm type: $cycleMode")

        when (cycleMode) {
            is PhotoWidgetCycleMode.Interval -> setupIntervalAlarm(cycleMode = cycleMode, appWidgetId = appWidgetId)

            is PhotoWidgetCycleMode.Schedule -> setupScheduleAlarm(cycleMode = cycleMode, appWidgetId = appWidgetId)

            is PhotoWidgetCycleMode.AdvancedSchedule -> setupAdvancedScheduleAlarm(
                cycleMode = cycleMode,
                appWidgetId = appWidgetId,
            )

            is PhotoWidgetCycleMode.Disabled -> return
        }
    }

    fun cancel(appWidgetId: Int) {
        Timber.i("Cancelling existing alarms for widget %s", mapOf("appWidgetId" to appWidgetId))

        alarmManager.cancel(
            TapActionPendingIntentFactory.getChangePhotoPendingIntent(context = context, appWidgetId = appWidgetId),
        )
        alarmManager.cancel(
            ExactRepeatingAlarmReceiver.pendingIntent(context = context, appWidgetId = appWidgetId),
        )
    }

    private fun setupIntervalAlarm(cycleMode: PhotoWidgetCycleMode.Interval, appWidgetId: Int) {
        val intervalMillis: Long = cycleMode.loopingInterval.run { timeUnit.toMillis(repeatInterval) }
        val nextCycleTime: Long = photoWidgetStorage.getWidgetNextCycleTime(appWidgetId = appWidgetId)
        val currentTimeMillis: Long = System.currentTimeMillis()
        val triggerAtMillis: Long = if (nextCycleTime > currentTimeMillis) {
            nextCycleTime
        } else {
            currentTimeMillis + intervalMillis
        }

        photoWidgetStorage.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = triggerAtMillis)

        if (canScheduleExactAlarms) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    /* type = */ AlarmManager.RTC_WAKEUP,
                    /* triggerAtMillis = */ triggerAtMillis,
                    /* operation = */
                    ExactRepeatingAlarmReceiver.pendingIntent(context = context, appWidgetId = appWidgetId),
                )
            } catch (_: SecurityException) {
                Timber.w("SecurityException: fallback to inexact alarm")

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
            /* type = */ AlarmManager.RTC_WAKEUP,
            /* triggerAtMillis = */ triggerAtMillis,
            /* intervalMillis = */ intervalMillis,
            /* operation = */
            TapActionPendingIntentFactory.getChangePhotoPendingIntent(context = context, appWidgetId = appWidgetId),
        )
    }

    private fun setupScheduleAlarm(cycleMode: PhotoWidgetCycleMode.Schedule, appWidgetId: Int) {
        if (cycleMode.triggers.isEmpty()) {
            Timber.w(
                "No triggers defined for widget %s. Skipping schedule alarm setup.",
                mapOf("appWidgetId" to appWidgetId),
            )
            return
        }

        val calendar: Calendar = Calendar.getInstance(TimeZone.getDefault())
        val currentHour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute: Int = calendar.get(Calendar.MINUTE)

        val sortedTriggers: List<Time> = cycleMode.triggers.sortedWith(
            comparator = compareBy({ it.hour }, { it.minute }),
        )

        var nextTrigger: Time? = null
        var advanceToNextDay = false

        for (trigger in sortedTriggers) {
            if (trigger.hour > currentHour || (trigger.hour == currentHour && trigger.minute > currentMinute)) {
                nextTrigger = trigger
                break
            }
        }

        if (nextTrigger == null) {
            nextTrigger = sortedTriggers.first()
            advanceToNextDay = true
        }

        calendar.apply {
            if (advanceToNextDay) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, nextTrigger.hour)
            set(Calendar.MINUTE, nextTrigger.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val pendingIntent: PendingIntent = ExactRepeatingAlarmReceiver.pendingIntent(
            context = context,
            appWidgetId = appWidgetId,
        )

        if (canScheduleExactAlarms) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    /* type = */ AlarmManager.RTC_WAKEUP,
                    /* triggerAtMillis = */ calendar.timeInMillis,
                    /* operation = */ pendingIntent,
                )
            } catch (_: SecurityException) {
                Timber.w("SecurityException: fallback to inexact alarm")
                setAlarm(triggerAtMillis = calendar.timeInMillis, pendingIntent = pendingIntent)
            }
        } else {
            setAlarm(triggerAtMillis = calendar.timeInMillis, pendingIntent = pendingIntent)
        }
    }

    private fun setupAdvancedScheduleAlarm(cycleMode: PhotoWidgetCycleMode.AdvancedSchedule, appWidgetId: Int) {
        if (cycleMode.schedule.isEmpty()) {
            Timber.w(
                "No schedule defined for widget %s. Skipping advanced schedule alarm.",
                mapOf("appWidgetId" to appWidgetId),
            )
            return
        }

        val calendar: Calendar = Calendar.getInstance(TimeZone.getDefault())
        val currentHour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute: Int = calendar.get(Calendar.MINUTE)

        val sortedEntries: List<Map.Entry<String, Time>> = cycleMode.schedule.entries.sortedWith(
            compareBy({ it.value.hour }, { it.value.minute }),
        )

        var nextEntry: Map.Entry<String, Time>? = null
        var advanceToNextDay = false

        for (entry in sortedEntries) {
            val (hour, minute) = entry.value
            if (hour > currentHour || (hour == currentHour && minute > currentMinute)) {
                nextEntry = entry
                break
            }
        }

        if (nextEntry == null) {
            nextEntry = sortedEntries.first()
            advanceToNextDay = true
        }

        calendar.apply {
            if (advanceToNextDay) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, nextEntry.value.hour)
            set(Calendar.MINUTE, nextEntry.value.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val pendingIntent: PendingIntent = ExactRepeatingAlarmReceiver.pendingIntent(
            context = context,
            appWidgetId = appWidgetId,
            nextPhotoId = nextEntry.key,
        )

        if (canScheduleExactAlarms) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    /* type = */ AlarmManager.RTC_WAKEUP,
                    /* triggerAtMillis = */ calendar.timeInMillis,
                    /* operation = */ pendingIntent,
                )
            } catch (_: SecurityException) {
                Timber.w("SecurityException: fallback to inexact alarm")
                setAlarm(triggerAtMillis = calendar.timeInMillis, pendingIntent = pendingIntent)
            }
        } else {
            setAlarm(triggerAtMillis = calendar.timeInMillis, pendingIntent = pendingIntent)
        }
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        alarmManager.setAndAllowWhileIdle(
            /* type = */ AlarmManager.RTC_WAKEUP,
            /* triggerAtMillis = */ triggerAtMillis,
            /* operation = */ pendingIntent,
        )
    }
}

class ExactRepeatingAlarmReceiver : EntryPointBroadcastReceiver() {

    override suspend fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.i("Working... %s", mapOf("appWidgetId" to intent.appWidgetId))

        val nextPhotoId: String? = intent.nextPhotoId

        entryPoint.run {
            if (nextPhotoId != null) {
                photoWidgetStorage().saveDisplayedPhoto(appWidgetId = intent.appWidgetId, photoId = nextPhotoId)
                PhotoWidgetProvider.update(context = context, appWidgetId = intent.appWidgetId)
            } else {
                cyclePhotoUseCase().invoke(appWidgetId = intent.appWidgetId)
                photoWidgetStorage().saveWidgetNextCycleTime(appWidgetId = intent.appWidgetId, nextCycleTime = null)
            }
            photoWidgetAlarmManager().setup(appWidgetId = intent.appWidgetId)
        }
    }

    companion object {

        private var Intent.nextPhotoId: String? by intentExtras()

        fun pendingIntent(
            context: Context,
            appWidgetId: Int,
            nextPhotoId: String? = null,
        ): PendingIntent {
            val intent = Intent(context, ExactRepeatingAlarmReceiver::class.java).apply {
                setIdentifierCompat("$appWidgetId")
                this.appWidgetId = appWidgetId
                this.nextPhotoId = nextPhotoId
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
