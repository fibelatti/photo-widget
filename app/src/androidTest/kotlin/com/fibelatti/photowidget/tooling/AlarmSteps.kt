package com.fibelatti.photowidget.tooling

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.fibelatti.photowidget.widget.RepeatingAlarmReceiver

private val ALARM_HEADER = Regex("""Alarm\{\S+ type \d+ origWhen (\d+) """)

/**
 * Returns the trigger times of the alarms the system holds for the app.
 *
 * `AlarmManager` can't list pending alarms, so they are read from `dumpsys alarm`. Each alarm is printed as a header
 * followed by its tag:
 *
 * ```
 * RTC_WAKEUP #24: Alarm{a904695 type 0 origWhen 1794216441006 whenElapsed 86951343 com.fibelatti.photowidget.debug}
 *   tag=*walarm*:com.fibelatti.photowidget.debug/com.fibelatti.photowidget.widget.RepeatingAlarmReceiver
 * ```
 *
 * `origWhen` is the trigger time requested by the app, also when the system falls back to an inexact
 * alarm. The dump format isn't a public API, and the example matches the output from API 36.
 */
fun scheduledAlarms(): List<Long> {
    val tag = "tag=*walarm*:${targetContext.packageName}/${RepeatingAlarmReceiver::class.java.name}"

    return executeDumpsysAlarm()
        .lineSequence()
        .zipWithNext()
        .mapNotNull { (header: String, next: String) ->
            val triggerAtMillis: Long = ALARM_HEADER.find(header)?.groupValues?.get(1)?.toLong()
                ?: return@mapNotNull null

            triggerAtMillis.takeIf { next.trim() == tag }
        }
        .toList()
}

private fun executeDumpsysAlarm(): String {
    val output: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand("dumpsys alarm")

    return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }
}

private const val NO_NEXT_CYCLE_TIME: Long = -1L

/**
 * Waits until the system holds an alarm for [widgetId] other than [previous], and returns its trigger time.
 *
 * The dump doesn't say which widget an alarm belongs to, so it's matched by the next cycle time the
 * widget saves when its interval alarm is set. That time is saved right before the alarm, so both are
 * awaited.
 */
fun ComposeTestRule.awaitAlarm(widgetId: Int, previous: Long? = null): Long {
    var triggerAtMillis: Long = NO_NEXT_CYCLE_TIME

    waitUntil(DEFAULT_TIMEOUT) {
        triggerAtMillis = widgetNextCycleTime(widgetId = widgetId)

        triggerAtMillis != NO_NEXT_CYCLE_TIME &&
            triggerAtMillis != previous &&
            triggerAtMillis in scheduledAlarms()
    }

    return triggerAtMillis
}
