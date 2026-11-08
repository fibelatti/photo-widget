package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoWidgetProvider : AppWidgetProvider() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Timber.i("Broadcast received %s", mapOf("action" to intent.action, "appWidgetId" to intent.appWidgetId))

        val action: Action = Action.fromValue(intent.action) ?: return
        val appWidgetId: Int = intent.appWidgetId

        // Keep the process at elevated priority while the resulting widget update (and its
        // optional crossfade animation) runs, then release it. The PendingResult is finished once
        // the work completes. The broadcast serial queue is freed as soon as the fade settles in
        // order to avoid delaying the next update.
        val pendingResult: PendingResult = goAsync()
        val finished = AtomicBoolean(false)
        val finishOnce: () -> Unit = {
            if (finished.compareAndSet(false, true)) {
                Timber.i("Finishing async receiver...")
                runCatching { pendingResult.finish() }
            }
        }

        val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
        val work: Job = entryPoint.coroutineScope().launch {
            entryPoint.photoWidgetProviderActionHandler().invoke(
                action = action,
                appWidgetId = appWidgetId,
                onRemoveActionHandled = { didRemove: Boolean ->
                    if (!coroutineContext.isActive) return@invoke
                    context.startActivity(
                        HeadlessFeedbackActivity.newIntent(
                            context = context,
                            message = context.getString(
                                if (didRemove) {
                                    R.string.photo_widget_remove_action_feedback_positive
                                } else {
                                    R.string.photo_widget_remove_action_feedback_negative
                                },
                            ),
                        ),
                    )
                },
            )
            // The action triggers a widget render that covers the crossfade.
            // Wait it out so the process stays alive until the fade ends.
            entryPoint.widgetProviders().awaitPhotoWidgetUpdate(appWidgetId = appWidgetId)
        }
        work.invokeOnCompletion { finishOnce() }

        // Safety net: never hold the broadcast to avoid ANRs if the awaited chain hangs.
        handler.postDelayed(delayInMillis = 3.seconds.inWholeMilliseconds) {
            if (work.isActive) {
                Timber.w("Async receiver timed out; finishing.")
                work.cancel()
            }
            finishOnce()
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.i("Update requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            val widgetProviders: WidgetProviders = entryPoint<PhotoWidgetEntryPoint>(context).widgetProviders()
            for (appWidgetId in appWidgetIds) {
                widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        Timber.i("Options changed by the system %s", mapOf("appWidgetId" to appWidgetId))

        handler.post {
            entryPoint<PhotoWidgetEntryPoint>(context).widgetProviders().updatePhotoWidget(appWidgetId = appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.i("Delete requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val storage: PhotoWidgetStorage = entryPoint.photoWidgetStorage()
            val alarmManager: PhotoWidgetAlarmManager = entryPoint.photoWidgetAlarmManager()

            for (appWidgetId in appWidgetIds) {
                storage.saveWidgetDeletionTimestamp(appWidgetId = appWidgetId, timestamp = System.currentTimeMillis())
                alarmManager.cancel(appWidgetId = appWidgetId)
                KeepAliveService.tearDownGif(appWidgetId = appWidgetId)
            }
        }
    }

    enum class Action(val value: String) {

        VIEW_NEXT_PHOTO(value = "ACTION_VIEW_NEXT_PHOTO"),
        VIEW_PREVIOUS_PHOTO(value = "ACTION_VIEW_PREVIOUS_PHOTO"),
        REMOVE_PHOTO(value = "ACTION_REMOVE_PHOTO"),
        ;

        companion object {

            fun fromValue(value: String?): Action? = entries.firstOrNull { it.value == value }
        }
    }
}
