package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import timber.log.Timber

/**
 * Renders a "transparent widget": a widget with no photos, only tap behavior. It reuses the shared
 * [PhotoWidget] model, storage, and configure activity, but has its own (photo-less) layout so the
 * rendering path skips all the photo decode / crop / cycle / alarm machinery of [PhotoWidgetProvider].
 */
class TransparentWidgetProvider : AppWidgetProvider() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.i("Update requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            val widgetProviders: WidgetProviders = entryPoint<PhotoWidgetEntryPoint>(context).widgetProviders()
            for (appWidgetId in appWidgetIds) {
                widgetProviders.updateTransparentWidget(appWidgetId = appWidgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        Timber.i("Options changed by the system %s", mapOf("appWidgetId" to appWidgetId))

        handler.post {
            entryPoint<PhotoWidgetEntryPoint>(context).widgetProviders()
                .updateTransparentWidget(appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.i("Delete requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            val storage = entryPoint<PhotoWidgetEntryPoint>(context).photoWidgetStorage()
            for (appWidgetId in appWidgetIds) {
                storage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = System.currentTimeMillis())
            }
        }
    }
}
