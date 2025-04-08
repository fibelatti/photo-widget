package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import timber.log.Timber

class DynamicBorderReceiver : EntryPointBroadcastReceiver() {

    override fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.d("Working...")

        val acceptedAction = Intent.ACTION_CONFIGURATION_CHANGED == intent.action ||
            Intent.ACTION_SCREEN_ON == intent.action

        if (!acceptedAction) {
            return
        }

        val ids = PhotoWidgetProvider.ids(context).ifEmpty {
            Timber.d("There are no widgets")
            return
        }

        entryPoint.runCatching {
            val photoWidgetStorage = photoWidgetStorage()
            for (id in ids) {
                val border = photoWidgetStorage.getWidgetBorder(appWidgetId = id)
                if (border is PhotoWidgetBorder.Dynamic) {
                    Timber.d("Updating widget with dynamic border (appWidgetId=$id)")
                    PhotoWidgetProvider.update(context = context, appWidgetId = id)
                }
            }
        }
    }

    companion object {

        fun register(context: Context) {
            val dynamicBorderReceiver = DynamicBorderReceiver()
            val intentFilter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED).apply {
                addAction(Intent.ACTION_SCREEN_ON)
            }

            context.registerReceiver(dynamicBorderReceiver, intentFilter)
        }
    }
}
