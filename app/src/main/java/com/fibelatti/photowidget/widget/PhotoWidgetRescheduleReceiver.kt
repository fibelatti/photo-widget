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
            (Intent.ACTION_PACKAGE_REPLACED == intent.action &&
                intent.data?.schemeSpecificPart == context.packageName)

        Timber.d("Reschedule received (isBoot=$isBoot, isUpdate=$isUpdate)")

        if (isBoot || isUpdate) {
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
                    Timber.d("Processing widget (id=$id, cycleMode=$cycleMode)")

                    if (cycleMode !is PhotoWidgetCycleMode.Disabled) {
                        photoWidgetAlarmManager.setup(appWidgetId = id)
                    }

                    PhotoWidgetProvider.update(context = context, appWidgetId = id)
                }
            }
        }
    }
}
