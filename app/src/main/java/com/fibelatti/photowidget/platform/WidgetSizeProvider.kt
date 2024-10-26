package com.fibelatti.photowidget.platform

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT

class WidgetSizeProvider(private val context: Context) {

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun getWidgetsSize(appWidgetId: Int, convertToPx: Boolean = false): Pair<Int, Int> {
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val width = getWidgetWidth(appWidgetId = appWidgetId, isPortrait = isPortrait)
        val height = getWidgetHeight(appWidgetId = appWidgetId, isPortrait = isPortrait)

        return if (convertToPx) {
            context.dip(width) to context.dip(height)
        } else {
            width to height
        }
    }

    private fun getWidgetWidth(appWidgetId: Int, isPortrait: Boolean): Int {
        return if (isPortrait) {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            getWidgetSizeInDp(appWidgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }
    }

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
