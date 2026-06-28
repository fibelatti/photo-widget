package com.fibelatti.photowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetPinningCache
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.platform.getMaxRemoteViewsBitmapMemory
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class PhotoWidgetProvider : AppWidgetProvider() {

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Timber.i("Broadcast received %s", mapOf("action" to intent.action, "appWidgetId" to intent.appWidgetId))

        val action: Action = Action.fromValue(intent.action) ?: return

        // Keep the process at elevated priority while the resulting update (and its crossfade
        // animation) runs, then release it. The work is dispatched asynchronously, so finish the
        // PendingResult after a fixed window covering the animation rather than blocking here.
        val pendingResult: PendingResult = goAsync()

        handler.post {
            entryPoint<PhotoWidgetEntryPoint>(context).photoWidgetProviderActionHandler().invoke(
                action = action,
                appWidgetId = intent.appWidgetId,
                onRemoveActionHandled = { didRemove: Boolean ->
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
        }

        handler.postDelayed(delayInMillis = 3.seconds.inWholeMilliseconds) {
            Timber.d("Finishing async receiver...")
            runCatching { pendingResult.finish() }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.i("Update requested by the system %s", mapOf("appWidgetIds" to appWidgetIds.toList()))

        handler.post {
            for (appWidgetId in appWidgetIds) {
                update(context = context, appWidgetId = appWidgetId)
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
            update(context = context, appWidgetId = appWidgetId)
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
                KeepAliveService.sendTearDownGifBroadcast(context = context, appWidgetId = appWidgetId)
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

    companion object {

        private val updateJobMap: MutableMap<Int, Job> = mutableMapOf()

        fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PhotoWidgetProvider::class.java))
            .toList()
            .also { Timber.d("Provider widget IDs: $it") }

        fun update(
            context: Context,
            appWidgetId: Int,
            allowCrossfade: Boolean = false,
            recoveryMode: Boolean = false,
        ) {
            Timber.i("Updating widget %s", mapOf("appWidgetId" to appWidgetId))

            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val entryPoint: PhotoWidgetEntryPoint = entryPoint(context)
            val coroutineScope: CoroutineScope = entryPoint.coroutineScope()
            val photoWidgetStorage: PhotoWidgetStorage = entryPoint.photoWidgetStorage()
            val pinningCache: PhotoWidgetPinningCache = entryPoint.photoWidgetPinningCache()
            val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase = entryPoint.loadPhotoWidgetUseCase()
            val exceptionReporter: ExceptionReporter = entryPoint.exceptionReporter()
            val crossfadeAnimator: PhotoWidgetCrossfadeAnimator = entryPoint.photoWidgetCrossfadeAnimator()

            val currentJob: Job? = updateJobMap[appWidgetId]
            Timber.d("Evaluating current update job %s", mapOf("isActive" to currentJob?.isActive))

            val newJob: Job = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Timeout to avoid hanging waiting more than what's acceptable
                currentJob?.let { job -> withTimeoutOrNull(timeMillis = 5_000L) { job.join() } }

                val photoWidget: PhotoWidget = pinningCache.pendingWidget
                    ?.takeIf { appWidgetId !in photoWidgetStorage.getKnownWidgetIds().first() }
                    ?.also { Timber.d("Updating using the pending widget data") }
                    ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId).first { !it.isLoading }

                val prepareCurrentPhotoUseCase: PrepareCurrentPhotoUseCase = entryPoint.prepareCurrentPhotoUseCase()
                val preparedCurrentPhoto: PreparedCurrentPhoto? = prepareCurrentPhotoUseCase(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    recoveryMode = recoveryMode,
                )

                if (preparedCurrentPhoto == null) {
                    Timber.e("Failed to prepare current photo")
                    val views: RemoteViews = PhotoWidgetRemoteViewsBuilder.buildErrorState(
                        context = context,
                        appWidgetId = appWidgetId,
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId)
                val isCyclePaused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)

                val (currentImageViewId: Int, _) = PhotoWidgetRemoteViewsBuilder.imageViewIdsFor(
                    photoWidget.aspectRatio,
                )

                val canCrossfade: Boolean = canCrossfade(
                    context = context,
                    photoWidget = photoWidget,
                    preparedCurrentPhoto = preparedCurrentPhoto,
                    allowCrossfade = allowCrossfade,
                    recoveryMode = recoveryMode,
                )

                val renderState = PhotoWidgetRemoteViewsBuilder.RenderState(
                    photoWidget = photoWidget,
                    preparedCurrentPhoto = preparedCurrentPhoto,
                    isLocked = isLocked,
                    isCyclePaused = isCyclePaused,
                )

                val initialViews: RemoteViews = PhotoWidgetRemoteViewsBuilder.build(
                    context = context,
                    appWidgetId = appWidgetId,
                    state = renderState,
                    mode = if (canCrossfade) {
                        PhotoWidgetRemoteViewsBuilder.RenderMode.CROSSFADE_START
                    } else {
                        PhotoWidgetRemoteViewsBuilder.RenderMode.DEFAULT
                    },
                )

                Timber.d("Invoking AppWidgetManager#updateAppWidget")

                // Cancel any in-flight crossfade so its trailing frames can't clobber this render.
                withContext(Dispatchers.Main) { crossfadeAnimator.cancel(appWidgetId) }

                try {
                    when {
                        photoWidget.source == PhotoWidgetSource.GIF -> {
                            appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                            KeepAliveService.sendSetupGifBroadcast(context = context, appWidgetId = appWidgetId)
                        }

                        canCrossfade -> {
                            // Settle from the same in-memory bitmap the fade used; rebuilding from the
                            // URI here would make the host re-decode and flash as the animation completes.
                            // Built up front so it can serve as the opaque fallback if starting the
                            // animation fails — the widget must never be left on the transparent start frame.
                            val finalViews: RemoteViews = PhotoWidgetRemoteViewsBuilder.build(
                                context = context,
                                appWidgetId = appWidgetId,
                                state = renderState,
                                mode = PhotoWidgetRemoteViewsBuilder.RenderMode.CROSSFADE_SETTLE,
                            )
                            appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                            try {
                                withContext(Dispatchers.Main) {
                                    crossfadeAnimator.runCrossfade(
                                        context = context,
                                        appWidgetManager = appWidgetManager,
                                        appWidgetId = appWidgetId,
                                        animatedImageViewId = currentImageViewId,
                                        finalViews = finalViews,
                                    )
                                }
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (ex: Exception) {
                                Timber.w(ex, "Crossfade failed to start; settling on the opaque frame")
                                appWidgetManager.updateAppWidget(appWidgetId, finalViews)
                            }
                        }

                        else -> {
                            appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                        }
                    }
                } catch (ex: IllegalArgumentException) {
                    if (!recoveryMode) {
                        update(
                            context = context,
                            appWidgetId = appWidgetId,
                            allowCrossfade = allowCrossfade,
                            recoveryMode = true,
                        )
                    } else {
                        exceptionReporter.collectReport(ex)
                        val views: RemoteViews = PhotoWidgetRemoteViewsBuilder.buildErrorState(
                            context = context,
                            appWidgetId = appWidgetId,
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }

            updateJobMap[appWidgetId] = newJob
            newJob.invokeOnCompletion {
                // Only remove if we're still the current job (avoid racing with a newer update)
                if (updateJobMap[appWidgetId] === newJob) updateJobMap.remove(appWidgetId)
            }
        }

        /**
         * Crossfade only when [allowCrossfade] (the caller deliberately switched photos via a tap
         * action or a scheduled alarm) and only for static photos when a previous photo exists and the
         * screen is on. System-driven refreshes, single-photo widgets, GIF playback, the first render
         * and recovery mode all fall back to a plain swap.
         *
         * The fade is rendered entirely from in-memory bitmaps (current + previous) so the host never
         * decodes a content URI mid-animation — that decode otherwise races the fade and makes it
         * jump. Both bitmaps share a single RemoteViews update, so only crossfade when their combined
         * size fits the host's bitmap budget; larger photos fall back to a plain URI swap, which also
         * keeps them under the transaction limit.
         */
        private fun canCrossfade(
            context: Context,
            photoWidget: PhotoWidget,
            preparedCurrentPhoto: PreparedCurrentPhoto,
            allowCrossfade: Boolean,
            recoveryMode: Boolean,
        ): Boolean {
            val previousBitmap: Bitmap? = preparedCurrentPhoto.previousBitmap
            val combinedBitmapBytes: Long = preparedCurrentPhoto.fallback.allocationByteCount.toLong() +
                (previousBitmap?.allocationByteCount?.toLong() ?: 0L)
            return allowCrossfade &&
                preparedCurrentPhoto.uri != null &&
                previousBitmap != null &&
                combinedBitmapBytes <= context.getMaxRemoteViewsBitmapMemory() &&
                isScreenInteractive(context = context) &&
                !recoveryMode &&
                photoWidget.source != PhotoWidgetSource.GIF
        }

        private fun isScreenInteractive(context: Context): Boolean {
            return context.getSystemService<PowerManager>()?.isInteractive == true
        }
    }
}
