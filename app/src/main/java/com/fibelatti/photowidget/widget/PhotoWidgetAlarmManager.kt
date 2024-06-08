package com.fibelatti.photowidget.widget

import android.app.AlarmManager
import android.content.Context
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
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
                    /* operation = */ PhotoWidgetProvider.flipPhotoPendingIntent(
                        context = context,
                        appWidgetId = appWidgetId,
                        rescheduleAlarm = true,
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

        val pendingIntent = PhotoWidgetProvider.flipPhotoPendingIntent(
            context = context,
            appWidgetId = appWidgetId,
        )

        alarmManager.cancel(pendingIntent)
    }
}
