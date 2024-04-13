package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * [BroadcastReceiver] to handle the callback from [AppWidgetManager.requestPinAppWidget].
 */
class PhotoWidgetPinnedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.appWidgetId
            .takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }
            // Workaround Samsung devices that fail to update the intent with the actual ID
            ?: PhotoWidgetProvider.ids(context = context).lastOrNull()
            // Exit early if the widget was not placed
            ?: return

        // A callback intent is required as it carries the widget data
        val callbackIntent = PhotoWidgetPinnedReceiver.callbackIntent?.get() ?: return

        // Reset the static fields once they have been consumed
        PhotoWidgetPinnedReceiver.callbackIntent = null
        preview = null

        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val saveUseCase = entryPoint.savePhotoWidgetUseCase()
        val coroutineScope = entryPoint.coroutineScope()

        coroutineScope.launch {
            // Persist the widget data since it was placed on the home screen
            saveUseCase(
                appWidgetId = widgetId,
                photoWidget = callbackIntent.photoWidget,
            )

            // Update the widget UI using the updated storage data
            PhotoWidgetProvider.update(
                context = context,
                appWidgetId = widgetId,
            )

            // Finally finish the configure activity since it's no longer needed
            val finishIntent = Intent(PhotoWidgetConfigureActivity.ACTION_FINISH).apply {
                this.appWidgetId = widgetId
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(finishIntent)
        }
    }

    companion object {

        /**
         * Workaround Samsung devices that fail to deliver the callback Intent provided to
         * `PendingIntent.getBroadcast`. Any caller that depends on this receiver should also
         * set this field, which will be used to retrieve the widget data.
         */
        var callbackIntent: WeakReference<Intent>? = null

        /**
         * Workaround to `AppWidgetProvider#onUpdate` being called with the new widget ID as the
         * user begins pinning the widget, but `PhotoWidgetPinnedReceiver` haven't being called
         * yet to move the content to the new directory.
         */
        var preview: WeakReference<RemoteViews>? = null
    }
}
