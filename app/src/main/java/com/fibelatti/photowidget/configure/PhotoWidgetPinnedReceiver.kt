package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.widget.PhotoWidgetProvider

/**
 * [BroadcastReceiver] to handle the callback from [AppWidgetManager.requestPinAppWidget].
 */
class PhotoWidgetPinnedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.appWidgetId

        // Exit early if the widget was not placed
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
        val saveUseCase = entryPoint.savePhotoWidgetUseCase()
        val storage = entryPoint.photoWidgetStorage()

        // Persist the widget data since it was placed on the home screen
        saveUseCase(
            appWidgetId = widgetId,
            enableLooping = intent.enableLooping,
            loopingInterval = intent.loopingInterval,
            aspectRatio = intent.aspectRatio,
            shapeId = intent.shapeId,
        )

        // Update the widget UI using the updated storage data
        PhotoWidgetProvider.update(
            context = context,
            appWidgetId = widgetId,
            photoPath = storage.getWidgetPhotos(widgetId).first(),
            aspectRatio = intent.aspectRatio,
            shapeId = intent.shapeId,
        )

        // Finally finish the configure activity since it's no longer needed
        val finishIntent = Intent(PhotoWidgetConfigureActivity.ACTION_FINISH).apply {
            this.appWidgetId = widgetId
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(finishIntent)
    }
}
