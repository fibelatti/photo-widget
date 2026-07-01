package com.fibelatti.photowidget.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.annotation.MainThread
import com.fibelatti.photowidget.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/**
 * Owns the crossfade animations used when a widget swaps to a new photo. The in-flight animators are
 * keyed by `appWidgetId` and confined to the main thread; [runCrossfade] asserts this at the single
 * create/cancel site, so the backing map needs no synchronization.
 *
 * A single instance is shared across the whole process so that a render (see
 * [PhotoWidgetProvider.update]) can [cancel] an animation started by a previous render of the same
 * widget before pushing its own frames.
 */
@Singleton
class PhotoWidgetCrossfadeAnimator @Inject constructor() {

    private val crossfadeAnimators: MutableMap<Int, ValueAnimator> = mutableMapOf()

    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * Cancels any in-flight crossfade for [appWidgetId] so its trailing frames can't clobber a newer
     * render. Must run on the main thread.
     */
    @MainThread
    fun cancel(appWidgetId: Int) {
        crossfadeAnimators.remove(appWidgetId)?.cancel()
    }

    /**
     * Runs the fade animation and suspends until it ends or is canceled.
     */
    @MainThread
    suspend fun runCrossfade(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        animatedImageViewId: Int,
        finalViews: RemoteViews,
    ) {
        check(Looper.getMainLooper().isCurrentThread) { "runCrossfade must run on the main thread" }

        Timber.d(
            "Running crossfade %s",
            mapOf("appWidgetId" to appWidgetId, "animatedImageViewId" to animatedImageViewId),
        )

        // Cancel any in-flight animation for this widget so a rapid tap-next restarts cleanly.
        cancel(appWidgetId)

        suspendCancellableCoroutine { continuation ->
            var canceled = false
            var lastAlpha: Int = -ALPHA_STEP

            val animator: ValueAnimator = ValueAnimator.ofInt(TRANSPARENT, OPAQUE).apply {
                duration = CROSSFADE_DURATION_MS
                addUpdateListener { animation ->
                    val alpha: Int = animation.animatedValue as Int
                    // Throttle to ~OPAQUE/ALPHA_STEP frames to limit IPC; always send the last frame.
                    if (alpha < OPAQUE && alpha - lastAlpha < ALPHA_STEP) return@addUpdateListener
                    lastAlpha = alpha

                    val partial = RemoteViews(context.packageName, R.layout.photo_widget)
                    partial.setInt(animatedImageViewId, METHOD_SET_IMAGE_ALPHA, alpha)

                    runCatching { appWidgetManager.partiallyUpdateAppWidget(appWidgetId, partial) }
                        .onFailure { Timber.w(it, "Failed partial crossfade update") }
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationCancel(animation: Animator) {
                            canceled = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            Timber.d(
                                "Crossfade animation ended %s",
                                mapOf(
                                    "appWidgetId" to appWidgetId,
                                    "animatedImageViewId" to animatedImageViewId,
                                    "canceled" to canceled,
                                ),
                            )

                            if (crossfadeAnimators[appWidgetId] === animation) {
                                crossfadeAnimators.remove(appWidgetId)
                            }

                            runCatching { appWidgetManager.updateAppWidget(appWidgetId, finalViews) }
                                .onFailure { Timber.w(it, "Failed to settle crossfade.") }

                            // onAnimationEnd fires after onAnimationCancel too, so this resumes both the
                            // natural-completion and the canceled paths exactly once.
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    },
                )
            }

            crossfadeAnimators[appWidgetId] = animator

            continuation.invokeOnCancellation {
                mainHandler.post { crossfadeAnimators.remove(appWidgetId)?.cancel() }
            }

            animator.start()
        }
    }

    companion object {

        internal const val METHOD_SET_IMAGE_ALPHA: String = "setImageAlpha"
        internal const val TRANSPARENT: Int = 0
        internal const val OPAQUE: Int = 255

        private const val CROSSFADE_DURATION_MS: Long = 1000

        // Only push a partial update every ~ALPHA_STEP alpha units (~21 frames over the fade).
        private const val ALPHA_STEP: Int = 12
    }
}
