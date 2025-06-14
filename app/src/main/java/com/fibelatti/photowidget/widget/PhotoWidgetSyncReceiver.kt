package com.fibelatti.photowidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.platform.EntryPointBroadcastReceiver
import com.fibelatti.photowidget.platform.setIdentifierCompat
import java.util.Calendar
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoWidgetSyncReceiver : EntryPointBroadcastReceiver() {

    override suspend fun doWork(context: Context, intent: Intent, entryPoint: PhotoWidgetEntryPoint) {
        Timber.d("Working...")

        val ids = PhotoWidgetProvider.ids(context).ifEmpty {
            Timber.d("There are no widgets")
            return
        }

        val photoWidgetStorage = entryPoint.photoWidgetStorage()
        val coroutineScope = entryPoint.coroutineScope()

        for (id in ids) {
            try {
                Timber.d("Processing widget (id=$id)")

                val source = photoWidgetStorage.getWidgetSource(appWidgetId = id)

                if (PhotoWidgetSource.DIRECTORY == source) {
                    coroutineScope.launch(NonCancellable) {
                        photoWidgetStorage.syncWidgetPhotos(appWidgetId = id)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing widget (id=$id)")
            }
        }
    }

    companion object {

        private const val CODE = 1001

        private fun pendingIntent(
            context: Context,
        ): PendingIntent {
            val intent = Intent(context, PhotoWidgetSyncReceiver::class.java).apply {
                setIdentifierCompat("$CODE")
            }

            return PendingIntent.getBroadcast(
                /* context = */
                context,
                /* requestCode = */
                CODE,
                /* intent = */
                intent,
                /* flags = */
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        fun setup(context: Context) {
            val alarmManager: AlarmManager = context.getSystemService() ?: return

            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            alarmManager.setRepeating(
                /* type = */
                AlarmManager.RTC,
                /* triggerAtMillis = */
                calendar.timeInMillis,
                /* intervalMillis = */
                1.days.inWholeMilliseconds,
                /* operation = */
                pendingIntent(context = context),
            )
        }
    }
}
