package com.fibelatti.photowidget.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import timber.log.Timber

abstract class EntryPointBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Broadcast received (action=${intent.action})")

        goAsync {
            doWork(context = context, intent = intent, entryPoint = entryPoint<PhotoWidgetEntryPoint>(context))
        }
    }

    abstract suspend fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint)
}
