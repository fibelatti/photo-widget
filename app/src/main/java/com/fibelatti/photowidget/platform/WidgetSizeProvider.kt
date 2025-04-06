package com.fibelatti.photowidget.platform

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.fibelatti.photowidget.R
import timber.log.Timber

class WidgetSizeProvider(context: Context) {

    private val appContext = context.applicationContext
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    fun getWidgetsSize(appWidgetId: Int, convertToPx: Boolean = false): Pair<Int, Int> {
        val isPortrait = appContext.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val isTablet = appContext.resources.getBoolean(R.bool.is_tablet)
        val measureAsPortrait = true // Temporarily always measuring as portrait to investigate the zooming issue

        val width = getWidgetWidth(appWidgetId = appWidgetId, isPortrait = measureAsPortrait)
        val height = getWidgetHeight(appWidgetId = appWidgetId, isPortrait = measureAsPortrait)

        return if (convertToPx) {
            appContext.dip(width) to appContext.dip(height)
        } else {
            width to height
        }.also {
            Timber.d(
                "Widget measured (" +
                    "appWidgetId=$appWidgetId," +
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

    @Suppress("SameParameterValue")
    private fun getWidgetWidth(appWidgetId: Int, isPortrait: Boolean): Int {
        return if (isPortrait) {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }
    }

    @Suppress("SameParameterValue")
    private fun getWidgetHeight(appWidgetId: Int, isPortrait: Boolean): Int {
        return if (isPortrait) {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        }
    }

    private fun getWidgetSizeInDp(appWidgetId: Int, key: String): Int {
        return appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(key, 0)
    }

    private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
