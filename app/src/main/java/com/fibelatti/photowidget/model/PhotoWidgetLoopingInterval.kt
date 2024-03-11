package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

@Parcelize
data class PhotoWidgetLoopingInterval(
    val repeatInterval: Long,
    val timeUnit: TimeUnit,
) : Parcelable {

    init {
        require(repeatInterval in MIN_VALUE..MAX_VALUE)
        require(TimeUnit.MINUTES == timeUnit || TimeUnit.HOURS == timeUnit)
    }

    fun toMinutes(): Long = timeUnit.toMinutes(repeatInterval)

    companion object {

        private const val MAX_VALUE = 30
        private const val MIN_VALUE = 1

        val RANGE = MIN_VALUE.toFloat()..MAX_VALUE.toFloat()

        val ONE_DAY = PhotoWidgetLoopingInterval(
            repeatInterval = 24,
            timeUnit = TimeUnit.HOURS,
        )

        fun Long.toLoopingInterval(): PhotoWidgetLoopingInterval {
            return if (this > MAX_VALUE) {
                PhotoWidgetLoopingInterval(
                    repeatInterval = TimeUnit.MINUTES.toHours(this),
                    timeUnit = TimeUnit.HOURS,
                )
            } else {
                PhotoWidgetLoopingInterval(
                    repeatInterval = TimeUnit.MINUTES.toMinutes(this),
                    timeUnit = TimeUnit.MINUTES,
                )
            }
        }
    }
}

/**
 * This enum used to act as a set of pre-defined intervals for flipping widgets. This has been migrated for a more
 * customizable approach where users can pick their intervals instead. This class is kept to allow migrating the
 * persisted data of any widgets that has been configured with it.
 */
@Suppress("Unused")
enum class LegacyPhotoWidgetLoopingInterval(
    val repeatInterval: Long,
    val timeUnit: TimeUnit,
) {

    ONE_DAY(
        repeatInterval = 24,
        timeUnit = TimeUnit.HOURS,
    ),
    TWELVE_HOURS(
        repeatInterval = 12,
        timeUnit = TimeUnit.HOURS,
    ),
    EIGHT_HOURS(
        repeatInterval = 8,
        timeUnit = TimeUnit.HOURS,
    ),
    SIX_HOURS(
        repeatInterval = 6,
        timeUnit = TimeUnit.HOURS,
    ),
    TWO_HOURS(
        repeatInterval = 2,
        timeUnit = TimeUnit.HOURS,
    ),
    ONE_HOUR(
        repeatInterval = 1,
        timeUnit = TimeUnit.HOURS,
    ),
    THIRTY_MINUTES(
        repeatInterval = 30,
        timeUnit = TimeUnit.MINUTES,
    ),
    FIFTEEN_MINUTES(
        repeatInterval = 15,
        timeUnit = TimeUnit.MINUTES,
    ),
}
