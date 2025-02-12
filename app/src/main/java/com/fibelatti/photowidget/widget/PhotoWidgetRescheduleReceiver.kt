package com.fibelatti.photowidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoWidgetRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isBoot = Intent.ACTION_BOOT_COMPLETED == intent.action
        val isUpdate = Intent.ACTION_MY_PACKAGE_REPLACED == intent.action ||
            (Intent.ACTION_PACKAGE_REPLACED == intent.action && intent.data?.schemeSpecificPart == context.packageName)
        val isManual = ACTION_RESCHEDULE == intent.action

        Timber.d("Reschedule received (isBoot=$isBoot, isUpdate=$isUpdate, isManual=$isManual)")

        if (isBoot || isUpdate || isManual) {
            val ids = PhotoWidgetProvider.ids(context).ifEmpty {
                Timber.d("There are no widgets")
                return
            }

            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val coroutineScope = entryPoint.coroutineScope()
            val photoWidgetStorage = entryPoint.photoWidgetStorage()
            val photoWidgetAlarmManager = entryPoint.photoWidgetAlarmManager()

            coroutineScope.launch {
                for (id in ids) {
                    val cycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = id)
                    val paused = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = id)
                    Timber.d("Processing widget (id=$id, cycleMode=$cycleMode, paused=$paused)")

                    if (cycleMode !is PhotoWidgetCycleMode.Disabled && !paused) {
                        photoWidgetAlarmManager.setup(appWidgetId = id)
                    }

                    PhotoWidgetProvider.update(context = context, appWidgetId = id)
                }
            }

            PhotoWidgetSyncReceiver.setup(context)
        }
    }

    companion object {

        const val ACTION_RESCHEDULE = "com.fibelatti.photowidget.action.RESCHEDULE"

        fun intent(context: Context): Intent = Intent(ACTION_RESCHEDULE).apply {
            setClassName(context.packageName, "com.fibelatti.photowidget.widget.PhotoWidgetRescheduleReceiver")
        }
    }
}
