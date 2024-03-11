package com.fibelatti.photowidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint

class PhotoWidgetRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val isBoot = Intent.ACTION_BOOT_COMPLETED == intent?.action
        val isUpdate = Intent.ACTION_MY_PACKAGE_REPLACED == intent?.action ||
            (Intent.ACTION_PACKAGE_REPLACED == intent?.action &&
                intent.data?.schemeSpecificPart == context?.packageName)

        if (context != null && (isBoot || isUpdate)) {
            val ids = PhotoWidgetProvider.ids(context).ifEmpty { return }

            val entryPoint = entryPoint<PhotoWidgetEntryPoint>(context)
            val loadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()
            val photoWidgetAlarmManager = entryPoint.photoWidgetAlarmManager()

            for (id in ids) {
                val widget = loadPhotoWidgetUseCase(appWidgetId = id)
                if (widget.loopingEnabled) {
                    photoWidgetAlarmManager.setup(
                        appWidgetId = id,
                        repeatInterval = widget.loopingInterval.repeatInterval,
                        timeUnit = widget.loopingInterval.timeUnit,
                    )
                }
            }
        }
    }
}
