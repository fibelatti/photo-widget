package com.fibelatti.photowidget.widget

import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.content.getSystemService
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetPinningCache
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetLoadStage
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.model.bitmapByteCount
import com.fibelatti.photowidget.model.displayValue
import com.fibelatti.photowidget.platform.ExceptionReporter
import com.fibelatti.photowidget.platform.KeepAliveService
import com.fibelatti.photowidget.platform.RemoteViewsBitmapMemoryCap
import com.fibelatti.photowidget.platform.TextFontLoader
import com.fibelatti.photowidget.platform.displayPixels
import com.fibelatti.photowidget.platform.getMaxCrossfadeBitmapMemory
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
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

/**
 * Queries and renders the widgets placed by this app providers.
 *
 * @see PhotoWidgetProvider
 * @see TransparentWidgetProvider
 */
interface WidgetProviders {

    /** IDs of the widgets currently placed by [PhotoWidgetProvider]. */
    fun photoWidgetIds(): List<Int>

    /** IDs of the widgets currently placed by [TransparentWidgetProvider]. */
    fun transparentWidgetIds(): List<Int>

    /**
     * Renders the photo widget [appWidgetId], returning the [Job] doing the work. Renders of the
     * same widget run one after the other, so a new render waits for the previous one to finish.
     *
     * The render runs on the application scope and outlives this call, so a caller whose own
     * lifetime bounds the process (e.g. a worker) must join the job to keep the render from being
     * cut short.
     *
     * @param allowCrossfade Whether the render may crossfade from the previous photo.
     */
    fun updatePhotoWidget(appWidgetId: Int, allowCrossfade: Boolean = false): Job

    /** Suspends until the in-flight render of the photo widget [appWidgetId], if any, completes. */
    suspend fun awaitPhotoWidgetUpdate(appWidgetId: Int)

    /** Renders the transparent widget [appWidgetId]. */
    fun updateTransparentWidget(appWidgetId: Int)
}

@Singleton
class DefaultWidgetProviders @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val pinningCache: PhotoWidgetPinningCache,
    private val loadPhotoWidgetUseCase: LoadPhotoWidgetUseCase,
    private val prepareCurrentPhotoUseCase: PrepareCurrentPhotoUseCase,
    private val textFontLoader: TextFontLoader,
    private val userPreferencesStorage: UserPreferencesStorage,
    private val exceptionReporter: ExceptionReporter,
    private val crossfadeAnimator: PhotoWidgetCrossfadeAnimator,
) : WidgetProviders {

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

    /**
     * The latest render job of each widget, which the next render of the same widget waits for.
     * Renders are requested from the main thread and from background threads, and jobs remove
     * themselves on completion from whichever thread completes them.
     */
    private val updateJobMap: ConcurrentHashMap<Int, Job> = ConcurrentHashMap()

    override fun photoWidgetIds(): List<Int> = ids(provider = PhotoWidgetProvider::class)

    override fun transparentWidgetIds(): List<Int> = ids(provider = TransparentWidgetProvider::class)

    private fun ids(provider: KClass<*>): List<Int> = appWidgetManager
        .getAppWidgetIds(ComponentName(context, provider.java))
        .toList()
        .also { Timber.d("Provider widget IDs %s", mapOf("provider" to provider.simpleName, "ids" to it)) }

    override fun updatePhotoWidget(appWidgetId: Int, allowCrossfade: Boolean): Job = updatePhotoWidget(
        appWidgetId = appWidgetId,
        allowCrossfade = allowCrossfade,
        recoveryAttempt = 0,
    )

    override suspend fun awaitPhotoWidgetUpdate(appWidgetId: Int) {
        updateJobMap[appWidgetId]?.join()
    }

    private fun updatePhotoWidget(appWidgetId: Int, allowCrossfade: Boolean, recoveryAttempt: Int): Job {
        Timber.i("Updating widget %s", mapOf("appWidgetId" to appWidgetId))

        // Reading the previous job and registering the new one happen atomically, so concurrent
        // requests for the same widget chain one after the other instead of both waiting on the
        // same previous job. The job starts lazily, after `compute` returns: running any of it
        // inside `compute` could request another render of the same widget, which the map
        // rejects as a recursive update.
        val newJob: Job? = updateJobMap.compute(appWidgetId) { _, currentJob: Job? ->
            Timber.d("Evaluating current update job %s", mapOf("isActive" to currentJob?.isActive))

            renderJob(
                appWidgetId = appWidgetId,
                allowCrossfade = allowCrossfade,
                recoveryAttempt = recoveryAttempt,
                currentJob = currentJob,
            )
        }
        requireNotNull(newJob)

        newJob.invokeOnCompletion { updateJobMap.remove(appWidgetId, newJob) }
        newJob.start()

        return newJob
    }

    private fun renderJob(appWidgetId: Int, allowCrossfade: Boolean, recoveryAttempt: Int, currentJob: Job?): Job {
        return coroutineScope.launch(start = CoroutineStart.LAZY) {
            // Cancel any in-flight crossfade from the previous render before joining it. Since
            // runCrossfade suspends for the animation's duration, the previous job stays active
            // until its fade settles; without this we'd block here waiting out a fade we're about
            // to replace. Cancelling settles it on its opaque frame and lets its job complete.
            withContext(Dispatchers.Main) { crossfadeAnimator.cancel(appWidgetId) }

            // Timeout to avoid hanging waiting more than what's acceptable
            currentJob?.let { job -> withTimeoutOrNull(timeMillis = 5_000L) { job.join() } }

            val photoWidget: PhotoWidget = pinningCache.pendingWidget
                ?.takeIf { appWidgetId !in photoWidgetStorage.getKnownWidgetIds().first() }
                ?.also { Timber.d("Updating using the pending widget data") }
                ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                    .first { it.loadStage >= PhotoWidgetLoadStage.CURRENT_PHOTO }

            // Resolved once so the label is measured for the render budget and drawn with the
            // same typeface. A font that takes longer to load triggers another render once ready,
            // after the current one settles so it doesn't cut a crossfade short.
            val labelTypeface: Typeface = textFontLoader.getWidgetTypeface(
                font = photoWidget.text.font,
                onLateLoad = {
                    awaitPhotoWidgetUpdate(appWidgetId = appWidgetId)
                    updatePhotoWidget(appWidgetId = appWidgetId)
                },
            )

            // Decided before preparing the photo to enable skipping unnecessary work (encoding
            // and decoding only needed by crossfade)
            val crossfadeIntent: Boolean = crossfadeIntent(
                photoWidget = photoWidget,
                allowCrossfade = allowCrossfade,
                recoveryAttempt = recoveryAttempt,
            )

            val preparedCurrentPhoto: PreparedCurrentPhoto? = try {
                prepareCurrentPhotoUseCase(
                    context = context,
                    appWidgetId = appWidgetId,
                    photoWidget = photoWidget,
                    labelTypeface = labelTypeface,
                    crossfadeIntent = crossfadeIntent,
                    recoveryAttempt = recoveryAttempt,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ex: Exception) {
                Timber.e(ex, "Error preparing current photo")
                exceptionReporter.collectReport(ex)
                null
            }

            if (preparedCurrentPhoto == null) {
                Timber.e("Failed to prepare current photo")
                val views: RemoteViews = PhotoWidgetRemoteViewsBuilder.buildErrorState(
                    context = context,
                    appWidgetId = appWidgetId,
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return@launch
            }

            // Run the crossfade path's deferred write here so it overlaps the fade instead of
            // gating it — the fade renders from in-memory bitmaps and never reads this file.
            // Launched as a child of this update job so the next update's `job.join()` waits
            // for it, keeping the previous-photo file chain ordered. `runCatching` so a write
            // failure degrades to a self-healing plain photo swap rather than canceling the
            // in-flight fade and leaving a half-faded frame.
            preparedCurrentPhoto.pendingWrite?.let { write ->
                launch(Dispatchers.IO) {
                    runCatching { write() }
                        .onFailure { Timber.w(it, "Deferred widget photo write failed") }
                }
            }

            val isLocked: Boolean = photoWidgetStorage.getWidgetLockedInApp(appWidgetId = appWidgetId)
            val isCyclePaused: Boolean = photoWidgetStorage.getWidgetCyclePaused(appWidgetId = appWidgetId)

            val renderState = PhotoWidgetRemoteViewsBuilder.RenderState(
                photoWidget = photoWidget,
                preparedCurrentPhoto = preparedCurrentPhoto,
                labelTypeface = labelTypeface,
                isLocked = isLocked,
                isCyclePaused = isCyclePaused,
            )

            val canCrossfade: Boolean = canCrossfade(
                crossfadeIntent = crossfadeIntent,
                photoWidget = photoWidget,
                labelTypeface = labelTypeface,
                preparedCurrentPhoto = preparedCurrentPhoto,
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

            Timber.i("Invoking AppWidgetManager#updateAppWidget")

            // Cancel any in-flight crossfade so its trailing frames can't clobber this render.
            withContext(Dispatchers.Main) { crossfadeAnimator.cancel(appWidgetId) }

            try {
                when {
                    photoWidget.source == PhotoWidgetSource.GIF -> {
                        appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                        KeepAliveService.setupGif(appWidgetId = appWidgetId)
                    }

                    canCrossfade -> {
                        // Settle from the full-resolution in-memory bitmap; rebuilding from the URI
                        // here would make the host re-decode and flash as the animation completes.
                        // The fade runs on downscaled bitmaps to fit the paired update, so settling
                        // on the full-res photo also sharpens back to full quality at rest. Built up
                        // front so it can serve as the opaque fallback if starting the animation
                        // fails — the widget must never be left on the transparent start frame.
                        val finalViews: RemoteViews = PhotoWidgetRemoteViewsBuilder.build(
                            context = context,
                            appWidgetId = appWidgetId,
                            state = renderState,
                            mode = PhotoWidgetRemoteViewsBuilder.RenderMode.CROSSFADE_SETTLE,
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, initialViews)

                        try {
                            withContext(Dispatchers.Main) {
                                val (currentImageViewId: Int, _) = PhotoWidgetRemoteViewsBuilder.imageViewIdsFor(
                                    photoWidget.aspectRatio,
                                )

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
                        } catch (ex: IllegalArgumentException) {
                            // The host rejected a frame for its size. Re-sending it would only be
                            // rejected again, so let the handler below learn the host's cap and
                            // re-render within it.
                            throw ex
                        } catch (ex: Exception) {
                            Timber.w(ex, "Crossfade failed; settling on the opaque frame")
                            appWidgetManager.updateAppWidget(appWidgetId, finalViews)
                        }
                    }

                    else -> {
                        appWidgetManager.updateAppWidget(appWidgetId, initialViews)
                    }
                }
            } catch (ex: IllegalArgumentException) {
                learnHostBitmapMemoryCap(exception = ex)

                if (recoveryAttempt < MAX_RECOVERY_ATTEMPTS) {
                    updatePhotoWidget(
                        appWidgetId = appWidgetId,
                        allowCrossfade = allowCrossfade,
                        recoveryAttempt = recoveryAttempt + 1,
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
    }

    /**
     * The crossfade preconditions that are known before the photo is prepared, so preparation can
     * decide whether to do the required extra work. A crossfade is intended only when:
     * - it is enabled (the user opted in via Widget settings), and
     * - the device has animations enabled, and
     * - the caller deliberately switched photos via a tap action or a scheduled alarm, and
     * - the screen is on, and
     * - it is a static-photo widget (not a GIF), and
     * - it is not a recovery render.
     *
     * Animations disabled at the OS level (developer options / battery-saver / accessibility) make
     * [ValueAnimator.areAnimatorsEnabled] return false; the fade depends on the animator's update
     * and end callbacks to settle the widget, so it would never run as expected.
     */
    private fun crossfadeIntent(
        photoWidget: PhotoWidget,
        allowCrossfade: Boolean,
        recoveryAttempt: Int,
    ): Boolean {
        return userPreferencesStorage.widgetEnableCrossfade &&
            ValueAnimator.areAnimatorsEnabled() &&
            allowCrossfade &&
            isScreenInteractive() &&
            photoWidget.source != PhotoWidgetSource.GIF &&
            recoveryAttempt == 0
    }

    /**
     * Records the bitmap memory cap reported by the given [exception] to use it for subsequent
     * update requests.
     */
    private fun learnHostBitmapMemoryCap(exception: IllegalArgumentException) {
        val reportedCap: Long = MAX_BITMAP_MEMORY_REGEX.find(exception.message.orEmpty())
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?: return

        val cap = RemoteViewsBitmapMemoryCap(
            bytes = reportedCap,
            displayPixels = context.displayPixels(),
        )

        if (userPreferencesStorage.remoteViewsBitmapMemoryCap != cap) {
            Timber.i("Saving host bitmap memory cap %s", mapOf("cap" to cap))
            userPreferencesStorage.remoteViewsBitmapMemoryCap = cap
        }
    }

    /**
     * Confirms a crossfade can actually run for this update, on top of [crossfadeIntent], once
     * the photo is prepared: the downscaled fade bitmap and a previous photo must both exist
     * (the first render of a widget has no previous). The persisted file/URI is deliberately
     * not required since the fade renders and settles using the in-memory bitmaps, so it can
     * start while the persist still runs in the background.
     *
     * The fade is rendered entirely from in-memory bitmaps so the host never decodes a content
     * URI mid-animation (which would race the fade and makes it jump). Both bitmaps travel in
     * a single RemoteViews update, so they are pre-sized by to fit the host's bitmap cap.
     *
     * The final check measures what the update will actually carry against the same budget the
     * pair was sized from, so a pair that came out larger than intended falls back to a plain
     * swap. Should sizing still be wrong on some host, [updatePhotoWidget] catches the resulting
     * `IllegalArgumentException` and re-renders with a smaller budget, so a mis-estimate
     * degrades the behavior rather than failing altogether.
     */
    private fun canCrossfade(
        crossfadeIntent: Boolean,
        photoWidget: PhotoWidget,
        labelTypeface: Typeface,
        preparedCurrentPhoto: PreparedCurrentPhoto,
    ): Boolean {
        if (!crossfadeIntent) return false

        val fadeBitmap: Bitmap = preparedCurrentPhoto.fadeBitmap ?: return false
        val previousBitmap: Bitmap = preparedCurrentPhoto.previousBitmap ?: return false

        val combinedBitmapBytes: Long = fadeBitmap.allocationByteCount.toLong() +
            previousBitmap.allocationByteCount.toLong() +
            photoWidget.text.bitmapByteCount(
                context = context,
                value = photoWidget.text.displayValue(photo = photoWidget.currentPhoto),
                typeface = labelTypeface,
            )

        return combinedBitmapBytes <= context.getMaxCrossfadeBitmapMemory()
    }

    private fun isScreenInteractive(): Boolean {
        return context.getSystemService<PowerManager>()?.isInteractive == true
    }

    override fun updateTransparentWidget(appWidgetId: Int) {
        Timber.i("Updating widget %s", mapOf("appWidgetId" to appWidgetId))

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val photoWidget: PhotoWidget = pinningCache.pendingWidget
                ?.takeIf { appWidgetId !in photoWidgetStorage.getKnownWidgetIds().first() }
                ?.also { Timber.d("Updating using the pending widget data") }
                ?: loadPhotoWidgetUseCase(appWidgetId = appWidgetId)
                    .first { it.loadStage >= PhotoWidgetLoadStage.CURRENT_PHOTO }

            val views = RemoteViews(context.packageName, R.layout.transparent_widget)

            views.setWidgetTapActions(
                context = context,
                appWidgetId = appWidgetId,
                photoWidget = photoWidget,
                isLocked = false,
                isCyclePaused = false,
                fillSidesWithCenter = true,
            )

            // Set the background unconditionally on every update: an already-hosted widget
            // reapplies RemoteViews actions onto its existing view tree, so the XML transparent
            // default is not restored. Without an explicit reset the highlight would stick once
            // enabled and never clear when the preference is turned off.
            val highlightColor: Int = if (userPreferencesStorage.highlightTransparentWidgets) {
                TRANSPARENT_WIDGET_HIGHLIGHT_COLOR
            } else {
                Color.TRANSPARENT
            }
            views.setInt(R.id.tap_actions_layout, "setBackgroundColor", highlightColor)

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (ex: IllegalArgumentException) {
                exceptionReporter.collectReport(ex)
            }
        }
    }

    private companion object {

        /**
         * How many times a rejected update is retried, each with a smaller budget, before the
         * widget gives up and shows the error state.
         */
        private const val MAX_RECOVERY_ATTEMPTS: Int = 2

        /**
         * Matches the cap in the message the widget host attaches when it rejects an update:
         * `RemoteViews for widget update exceeds maximum bitmap memory usage
         * (used: 8128512, max: 6912000)`.
         */
        private val MAX_BITMAP_MEMORY_REGEX = Regex("max: (\\d+)")

        /**
         * Semi-transparent neutral scrim applied to transparent widgets while the user has enabled
         * the "highlight transparent widgets" preference, so they can be spotted and rearranged on
         * the home screen.
         */
        private const val TRANSPARENT_WIDGET_HIGHLIGHT_COLOR: Int = 0x66808080
    }
}
