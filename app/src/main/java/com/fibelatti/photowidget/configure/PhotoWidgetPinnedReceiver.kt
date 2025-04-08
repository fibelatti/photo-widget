package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.platform.EntryPointBroadcastReceiver
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [BroadcastReceiver] to handle the callback from [AppWidgetManager.requestPinAppWidget].
 */
class PhotoWidgetPinnedReceiver : EntryPointBroadcastReceiver() {

    override fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.d("Working... (appWidgetId=${intent.appWidgetId})")

        val widgetId = intent.appWidgetId
            .takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }
            // Workaround Samsung devices that fail to update the intent with the actual ID
            ?: PhotoWidgetProvider.ids(context = context).lastOrNull()
            // Exit early if the widget was not placed
            ?: return

        val pinningCache = entryPoint.photoWidgetPinningCache()

        // The widget data is missing, it's impossible to continue
        val photoWidget = pinningCache.consume() ?: return

        Timber.d("New widget ID: $widgetId")

        val saveUseCase = entryPoint.savePhotoWidgetUseCase()
        val coroutineScope = entryPoint.coroutineScope()

        coroutineScope.launch {
            // Persist the widget data since it was placed on the home screen
            saveUseCase(appWidgetId = widgetId, photoWidget = photoWidget)

            // Update the widget UI using the updated storage data
            PhotoWidgetProvider.update(context = context, appWidgetId = widgetId)

            // Finally finish the configure activity since it's no longer needed
            val finishIntent = Intent(PhotoWidgetConfigureActivity.ACTION_FINISH).apply {
                this.appWidgetId = widgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(finishIntent)
        }
    }
}
