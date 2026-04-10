package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import timber.log.Timber

class ConfigurationChangedReceiver : EntryPointBroadcastReceiver() {

    override suspend fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.i("Working...")

        val acceptedAction: Boolean = Intent.ACTION_CONFIGURATION_CHANGED == intent.action

        if (!acceptedAction) {
            return
        }

        val ids = PhotoWidgetProvider.ids(context).ifEmpty {
            Timber.d("There are no widgets")
            return
        }

        for (id in ids) {
            currentCoroutineContext().ensureActive()
            try {
                Timber.d("Processing widget (id=$id)")

                PhotoWidgetProvider.update(context = context, appWidgetId = id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error processing widget (id=$id)")
            }
        }
    }

    companion object {

        fun register(context: Context) {
            val receiver = ConfigurationChangedReceiver()
            val intentFilter = IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED)

            context.registerReceiver(receiver, intentFilter)
        }
    }
}
