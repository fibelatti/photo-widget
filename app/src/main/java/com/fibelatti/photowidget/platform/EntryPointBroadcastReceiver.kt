package com.fibelatti.photowidget.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import timber.log.Timber

abstract class EntryPointBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Broadcast received (action=${intent.action})")

        Handler(Looper.getMainLooper()).postDelayed(delayInMillis = 300) {
            doWork(context, intent, entryPoint<PhotoWidgetEntryPoint>(context))
        }
    }

    abstract fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint)
}
