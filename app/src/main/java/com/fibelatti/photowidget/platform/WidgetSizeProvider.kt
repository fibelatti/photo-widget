package com.fibelatti.photowidget.platform

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.os.Bundle
import com.fibelatti.photowidget.R
import timber.log.Timber

class WidgetSizeProvider(context: Context) {

    private val resources: Resources = context.applicationContext.resources

    fun getWidgetsSize(
        widgetOptions: Bundle,
        convertToPx: Boolean = false,
    ): Pair<Int, Int> {
        val isPortrait = resources.configuration.orientation == ORIENTATION_PORTRAIT
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        val measureAsPortrait = isPortrait || !isTablet

        val width = getWidgetWidth(widgetOptions = widgetOptions, isPortrait = measureAsPortrait)
        val height = getWidgetHeight(widgetOptions = widgetOptions, isPortrait = measureAsPortrait)

        return if (convertToPx) {
            resources.dip(width) to resources.dip(height)
        } else {
            width to height
        }.also {
            Timber.d(
                "Widget measured (" +
                    "convertToPx=$convertToPx," +
                    "isPortrait=$isPortrait," +
                    "isTablet=$isTablet," +
                    "measureAsPortrait=$measureAsPortrait," +
                    "width=${it.first}," +
                    "height=${it.second}" +
                    ")",
            )
        }
    }

    private fun getWidgetWidth(widgetOptions: Bundle, isPortrait: Boolean): Int {
        return if (isPortrait) {
            getWidgetSizeInDp(widgetOptions, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            getWidgetSizeInDp(widgetOptions, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }
    }

    private fun getWidgetHeight(widgetOptions: Bundle, isPortrait: Boolean): Int {
        return if (isPortrait) {
            getWidgetSizeInDp(widgetOptions, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            getWidgetSizeInDp(widgetOptions, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        }
    }

    private fun getWidgetSizeInDp(widgetOptions: Bundle, key: String): Int {
        return widgetOptions.getInt(key, 0)
    }

    private fun Resources.dip(value: Int): Int = (value * displayMetrics.density).toInt()
}
