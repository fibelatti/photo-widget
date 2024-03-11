package com.fibelatti.photowidget.widget

import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoWidgetAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager by lazy { requireNotNull(context.getSystemService()) }

    fun setup(appWidgetId: Int, repeatInterval: Long, timeUnit: TimeUnit) {
        val interval = timeUnit.toMillis(repeatInterval)

        alarmManager.setRepeating(
            /* type = */ AlarmManager.ELAPSED_REALTIME,
            /* triggerAtMillis = */ SystemClock.elapsedRealtime() + interval,
            /* intervalMillis = */ interval,
            /* operation = */ PhotoWidgetProvider.flipPhotoPendingIntent(context, appWidgetId),
        )
    }

    fun cancel(appWidgetId: Int) {
        alarmManager.cancel(
            /* operation = */ PhotoWidgetProvider.flipPhotoPendingIntent(context, appWidgetId),
        )
    }
}
