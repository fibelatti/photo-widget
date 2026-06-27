package com.fibelatti.photowidget.widget

import android.app.PendingIntent
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
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.PhotoWidgetPinningCache
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.model.textToBitmap
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.platform.getMaxRemoteViewsBitmapMemory
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
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
                    val views: RemoteViews = setErrorState(
                        remoteViews = RemoteViews(context.packageName, R.layout.photo_widget),
                        context = context,
                        appWidgetId = appWidgetId,
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId)
                val isCyclePaused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)

                val (currentImageViewId: Int, previousImageViewId: Int) = imageViewIdsFor(photoWidget.aspectRatio)

                // Crossfade only for static photos when a previous photo exists and the screen is on.
                // GIF playback, the first render and recovery mode fall back to a plain swap.
                //
                // The fade is rendered entirely from in-memory bitmaps (current + previous) so the host
                // never decodes a content URI mid-animation — that decode otherwise races the fade and
                // makes it jump. Both bitmaps share a single RemoteViews update, so only crossfade when
                // their combined size fits the host's bitmap budget; larger photos fall back to a plain
                // URI swap, which also keeps them under the transaction limit.
                val previousBitmap: Bitmap? = preparedCurrentPhoto.previousBitmap
                val combinedBitmapBytes: Long = preparedCurrentPhoto.fallback.allocationByteCount.toLong() +
                    (previousBitmap?.allocationByteCount?.toLong() ?: 0L)
                val canCrossfade: Boolean = preparedCurrentPhoto.uri != null &&
                    previousBitmap != null &&
                    combinedBitmapBytes <= context.getMaxRemoteViewsBitmapMemory() &&
                    isScreenInteractive(context = context) &&
                    !recoveryMode &&
                    photoWidget.source != PhotoWidgetSource.GIF

                val initialViews: RemoteViews = buildRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    preparedCurrentPhoto = preparedCurrentPhoto,
                    currentImageViewId = currentImageViewId,
                    previousImageViewId = previousImageViewId,
                    isLocked = isLocked,
                    isCyclePaused = isCyclePaused,
                    setupCrossfade = canCrossfade,
                    renderCurrentFromBitmap = canCrossfade,
                )

                Timber.d("Invoking AppWidgetManager#updateAppWidget")

                // Cancel any in-flight crossfade so its trailing frames can't clobber this render.
                withContext(Dispatchers.Main) { crossfadeAnimator.cancel(appWidgetId) }

                try {
                    appWidgetManager.updateAppWidget(appWidgetId, initialViews)

                    when {
                        photoWidget.source == PhotoWidgetSource.GIF -> {
                            KeepAliveService.sendSetupGifBroadcast(context = context, appWidgetId = appWidgetId)
                        }

                        canCrossfade -> {
                            // Settle from the same in-memory bitmap the fade used; rebuilding from the
                            // URI here would make the host re-decode and flash as the animation completes.
                            val finalViews: RemoteViews = buildRemoteViews(
                                context = context,
                                appWidgetId = appWidgetId,
                                photoWidget = photoWidget,
                                preparedCurrentPhoto = preparedCurrentPhoto,
                                currentImageViewId = currentImageViewId,
                                previousImageViewId = previousImageViewId,
                                isLocked = isLocked,
                                isCyclePaused = isCyclePaused,
                                setupCrossfade = false,
                                renderCurrentFromBitmap = true,
                            )
                            withContext(Dispatchers.Main) {
                                crossfadeAnimator.runCrossfade(
                                    context = context,
                                    appWidgetManager = appWidgetManager,
                                    appWidgetId = appWidgetId,
                                    animatedImageViewId = currentImageViewId,
                                    finalViews = finalViews,
                                )
                            }
                        }
                    }
                } catch (ex: IllegalArgumentException) {
                    if (!recoveryMode) {
                        update(context = context, appWidgetId = appWidgetId, recoveryMode = true)
                    } else {
                        exceptionReporter.collectReport(ex)
                        val views: RemoteViews = setErrorState(
                            remoteViews = RemoteViews(context.packageName, R.layout.photo_widget),
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

        // region RemoteViews

        /**
         * Returns the (current, previous) image view IDs for the given aspect ratio. The two views
         * share the same scaleType so they can crossfade; `setScaleType` is not a remotable method,
         * hence the dedicated pair per aspect ratio.
         */
        private fun imageViewIdsFor(aspectRatio: PhotoWidgetAspectRatio): Pair<Int, Int> {
            return if (aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
                R.id.iv_widget_fill to R.id.iv_widget_fill_prev
            } else {
                R.id.iv_widget to R.id.iv_widget_prev
            }
        }

        private fun isScreenInteractive(context: Context): Boolean {
            return context.getSystemService<PowerManager>()?.isInteractive == true
        }

        private fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            photoWidget: PhotoWidget,
            preparedCurrentPhoto: PreparedCurrentPhoto,
            currentImageViewId: Int,
            previousImageViewId: Int,
            isLocked: Boolean,
            isCyclePaused: Boolean,
            setupCrossfade: Boolean,
            renderCurrentFromBitmap: Boolean,
        ): RemoteViews {
            Timber.d("Building remote views %s", mapOf("setupCrossfade" to setupCrossfade))
            return RemoteViews(context.packageName, R.layout.photo_widget).apply {
                setViewVisibility(R.id.placeholder_layout, View.GONE)
                setViewVisibility(R.id.iv_widget, View.GONE)
                setViewVisibility(R.id.iv_widget_fill, View.GONE)
                setViewVisibility(R.id.iv_widget_prev, View.GONE)
                setViewVisibility(R.id.iv_widget_fill_prev, View.GONE)

                setViewVisibility(currentImageViewId, View.VISIBLE)
                // The crossfade path forces the in-memory bitmap so the host has no URI to decode
                // mid-animation; plain renders use the URI to stay under the transaction limit.
                if (!renderCurrentFromBitmap && preparedCurrentPhoto.uri != null) {
                    setImageViewUri(currentImageViewId, preparedCurrentPhoto.uri)
                } else {
                    setImageViewBitmap(currentImageViewId, preparedCurrentPhoto.fallback)
                }

                setPadding(
                    remoteViews = this,
                    viewId = currentImageViewId,
                    context = context,
                    padding = photoWidget.padding,
                    verticalOffset = photoWidget.verticalOffset,
                    horizontalOffset = photoWidget.horizontalOffset,
                )

                if (setupCrossfade && preparedCurrentPhoto.previousBitmap != null) {
                    // The new photo (top) starts transparent and fades in over the previous photo
                    // (bottom); the animation drives the top view's alpha. See runCrossfade.
                    setViewVisibility(previousImageViewId, View.VISIBLE)
                    setImageViewBitmap(previousImageViewId, preparedCurrentPhoto.previousBitmap)
                    setInt(
                        previousImageViewId,
                        PhotoWidgetCrossfadeAnimator.METHOD_SET_IMAGE_ALPHA,
                        PhotoWidgetCrossfadeAnimator.OPAQUE,
                    )
                    setInt(
                        currentImageViewId,
                        PhotoWidgetCrossfadeAnimator.METHOD_SET_IMAGE_ALPHA,
                        PhotoWidgetCrossfadeAnimator.TRANSPARENT,
                    )
                    setPadding(
                        remoteViews = this,
                        viewId = previousImageViewId,
                        context = context,
                        padding = photoWidget.padding,
                        verticalOffset = photoWidget.verticalOffset,
                        horizontalOffset = photoWidget.horizontalOffset,
                    )
                }

                setText(
                    remoteViews = this,
                    context = context,
                    photoWidgetText = photoWidget.text,
                )

                setWidgetTapActions(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    isLocked = isLocked,
                    isCyclePaused = isCyclePaused,
                )
            }
        }

        private fun setText(
            remoteViews: RemoteViews,
            context: Context,
            photoWidgetText: PhotoWidgetText,
        ) {
            when (photoWidgetText) {
                is PhotoWidgetText.None -> {
                    remoteViews.setViewVisibility(R.id.iv_widget_label, View.GONE)
                }

                is PhotoWidgetText.Label -> {
                    val bitmap: Bitmap = photoWidgetText.textToBitmap(context = context)
                    val bottomPadding: Int = abs(photoWidgetText.verticalOffset)
                        .times(context.resources.displayMetrics.density)
                        .roundToInt()

                    remoteViews.setViewVisibility(R.id.iv_widget_label, View.VISIBLE)
                    remoteViews.setImageViewBitmap(R.id.iv_widget_label, bitmap)
                    remoteViews.setViewPadding(
                        /* viewId = */ R.id.iv_widget_label,
                        /* left = */ 0,
                        /* top = */ 0,
                        /* right = */ 0,
                        /* bottom = */ bottomPadding,
                    )
                }
            }
        }

        private fun setPadding(
            remoteViews: RemoteViews,
            viewId: Int,
            context: Context,
            padding: Int,
            verticalOffset: Int,
            horizontalOffset: Int,
        ) {
            var paddingLeft: Int = padding
            var paddingTop: Int = padding
            var paddingRight: Int = padding
            var paddingBottom: Int = padding

            when {
                horizontalOffset > 0 -> paddingLeft = padding + horizontalOffset
                horizontalOffset < 0 -> paddingRight = padding + abs(horizontalOffset)
            }

            when {
                verticalOffset > 0 -> paddingTop = padding + verticalOffset
                verticalOffset < 0 -> paddingBottom = padding + abs(verticalOffset)
            }

            val applyDimension: (Int) -> Int = { value: Int ->
                (value * context.resources.displayMetrics.density * PhotoWidget.POSITIONING_MULTIPLIER).roundToInt()
            }

            remoteViews.setViewPadding(
                /* viewId = */ viewId,
                /* left = */ applyDimension(paddingLeft),
                /* top = */ applyDimension(paddingTop),
                /* right = */ applyDimension(paddingRight),
                /* bottom = */ applyDimension(paddingBottom),
            )
        }

        private fun setErrorState(
            remoteViews: RemoteViews,
            context: Context,
            appWidgetId: Int,
        ): RemoteViews {
            val clickIntent = PhotoWidgetConfigureActivity.editWidgetIntent(
                context = context,
                appWidgetId = appWidgetId,
            )

            val pendingIntent = PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */ appWidgetId,
                /* intent = */ clickIntent,
                /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            return remoteViews.apply {
                setViewVisibility(R.id.placeholder_layout, View.VISIBLE)
                setViewVisibility(R.id.iv_widget, View.GONE)
                setViewVisibility(R.id.iv_widget_fill, View.GONE)
                setViewVisibility(R.id.tap_actions_layout, View.GONE)

                setImageViewResource(R.id.iv_placeholder, R.drawable.ic_file_not_found)
                setTextViewText(R.id.tv_placeholder, context.getString(R.string.photo_widget_host_failed))

                setOnClickPendingIntent(R.id.placeholder_layout, pendingIntent)
            }
        }
        // endregion RemoteViews
    }
}
