package com.fibelatti.photowidget.model

import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import java.util.concurrent.TimeUnit

enum class PhotoWidgetLoopingInterval(
    val repeatInterval: Long,
    val timeUnit: TimeUnit,
    @StringRes val title: Int,
) {

    ONE_DAY(
        repeatInterval = 24,
        timeUnit = TimeUnit.HOURS,
        title = R.string.photo_widget_configure_interval_one_day,
    ),
    TWELVE_HOURS(
        repeatInterval = 12,
        timeUnit = TimeUnit.HOURS,
        title = R.string.photo_widget_configure_interval_twelve_hours,
    ),
    SIX_HOURS(
        repeatInterval = 6,
        timeUnit = TimeUnit.HOURS,
        title = R.string.photo_widget_configure_interval_six_hours,
    ),
    ONE_HOUR(
        repeatInterval = 1,
        timeUnit = TimeUnit.HOURS,
        title = R.string.photo_widget_configure_interval_one_hour,
    ),
    THIRTY_MINUTES(
        repeatInterval = 30,
        timeUnit = TimeUnit.MINUTES,
        title = R.string.photo_widget_configure_interval_thirty_minutes,
    ),
}
