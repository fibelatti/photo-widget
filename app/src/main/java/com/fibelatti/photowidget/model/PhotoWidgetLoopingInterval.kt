package com.fibelatti.photowidget.model

import android.os.Parcelable
import java.util.concurrent.TimeUnit
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidgetLoopingInterval(
    val repeatInterval: Long,
    val timeUnit: TimeUnit,
) : Parcelable {

    fun toMinutes(): Long = timeUnit.toMinutes(repeatInterval)

    fun toSeconds(): Long = timeUnit.toSeconds(repeatInterval)

    fun range(): ClosedFloatingPointRange<Float> {
        return when (timeUnit) {
            TimeUnit.SECONDS -> MIN_SECONDS.toFloat()..MAX_DEFAULT.toFloat()
            TimeUnit.HOURS -> MIN_DEFAULT.toFloat()..MAX_HOURS.toFloat()
            else -> MIN_DEFAULT.toFloat()..MAX_DEFAULT.toFloat()
        }
    }

    companion object {

        const val MAX_DEFAULT: Long = 30
        const val MAX_HOURS: Long = 24
        const val MIN_DEFAULT: Long = 1
        const val MIN_SECONDS: Long = 10

        val ONE_DAY = PhotoWidgetLoopingInterval(
            repeatInterval = 24,
            timeUnit = TimeUnit.HOURS,
        )

        fun Long.minutesToLoopingInterval(): PhotoWidgetLoopingInterval {
            return if (this <= MAX_DEFAULT) {
                PhotoWidgetLoopingInterval(
                    repeatInterval = TimeUnit.MINUTES.toMinutes(this),
                    timeUnit = TimeUnit.MINUTES,
                )
            } else {
                PhotoWidgetLoopingInterval(
                    repeatInterval = TimeUnit.MINUTES.toHours(this),
                    timeUnit = TimeUnit.HOURS,
                )
            }
        }

        fun Long.secondsToLoopingInterval(): PhotoWidgetLoopingInterval {
            return when {
                this <= MAX_DEFAULT -> {
                    PhotoWidgetLoopingInterval(
                        repeatInterval = TimeUnit.SECONDS.toSeconds(this),
                        timeUnit = TimeUnit.SECONDS,
                    )
                }

                this <= TimeUnit.MINUTES.toSeconds(MAX_DEFAULT) -> {
                    PhotoWidgetLoopingInterval(
                        repeatInterval = TimeUnit.SECONDS.toMinutes(this),
                        timeUnit = TimeUnit.MINUTES,
                    )
                }

                else -> {
                    PhotoWidgetLoopingInterval(
                        repeatInterval = TimeUnit.SECONDS.toHours(this),
                        timeUnit = TimeUnit.HOURS,
                    )
                }
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
