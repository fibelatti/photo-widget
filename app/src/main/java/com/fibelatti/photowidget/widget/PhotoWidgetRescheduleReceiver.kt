package com.fibelatti.photowidget.widget

import android.content.Context
import android.content.Intent
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.platform.EntryPointBroadcastReceiver
import timber.log.Timber

class PhotoWidgetRescheduleReceiver : EntryPointBroadcastReceiver() {

    override suspend fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.d("Working...")

        val isBoot = Intent.ACTION_BOOT_COMPLETED == intent.action ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED == intent.action
        val isUpdate = Intent.ACTION_MY_PACKAGE_REPLACED == intent.action ||
            (Intent.ACTION_PACKAGE_REPLACED == intent.action && intent.data?.schemeSpecificPart == context.packageName)
        val isManual = ACTION_RESCHEDULE == intent.action

        if (isBoot || isUpdate || isManual) {
            val ids = PhotoWidgetProvider.ids(context).ifEmpty {
                Timber.d("There are no widgets")
                return
            }

            PhotoWidgetSyncReceiver.setup(context)

            val photoWidgetStorage = entryPoint.photoWidgetStorage()
            val photoWidgetAlarmManager = entryPoint.photoWidgetAlarmManager()

            for (id in ids) {
                val cycleMode = photoWidgetStorage.getWidgetCycleMode(appWidgetId = id)
                val isLocked = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = id)
                val isPaused = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = id)
                Timber.d("Processing widget (id=$id,cycleMode=$cycleMode,isLocked=$isLocked,isPaused=$isPaused)")

                if (cycleMode !is PhotoWidgetCycleMode.Disabled && !isLocked && !isPaused) {
                    photoWidgetAlarmManager.setup(appWidgetId = id)
                }

                PhotoWidgetProvider.update(context = context, appWidgetId = id)
            }
        }
    }

    companion object {

        private const val ACTION_RESCHEDULE = "com.fibelatti.photowidget.action.RESCHEDULE"

        fun intent(context: Context): Intent = Intent(ACTION_RESCHEDULE).apply {
            setClassName(context.packageName, "com.fibelatti.photowidget.widget.PhotoWidgetRescheduleReceiver")
        }
    }
}
