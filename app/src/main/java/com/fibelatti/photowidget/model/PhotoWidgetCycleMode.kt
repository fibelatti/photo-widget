package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetCycleMode : Parcelable {

    @Parcelize
    data class Interval(val loopingInterval: PhotoWidgetLoopingInterval) : PhotoWidgetCycleMode

    @Parcelize
    data class Schedule(val triggers: Set<Time>) : PhotoWidgetCycleMode

    @Parcelize
    data object Disabled : PhotoWidgetCycleMode

    companion object {

        val DEFAULT = Interval(loopingInterval = PhotoWidgetLoopingInterval.ONE_DAY)
    }
}

@Parcelize
data class Time(val hour: Int, val minute: Int) : Parcelable {

    fun asString(): String = "${format(hour)}:${format(minute)}"

    private fun format(value: Int): String = value.toString().padStart(2, '0')

    companion object {

        fun fromString(value: String): Time = value.split(":").run {
            Time(hour = get(0).toInt(), minute = get(1).toInt())
        }
    }
}
