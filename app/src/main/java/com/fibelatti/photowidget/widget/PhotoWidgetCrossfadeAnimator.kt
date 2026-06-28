package com.fibelatti.photowidget.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import android.widget.RemoteViews
import androidx.annotation.MainThread
import com.fibelatti.photowidget.R
import javax.inject.Inject
import javax.inject.Singleton
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

    // Confined to the main thread; runCrossfade asserts this at the single create/cancel site.
    private val crossfadeAnimators: MutableMap<Int, ValueAnimator> = mutableMapOf()

    /**
     * Cancels any in-flight crossfade for [appWidgetId] so its trailing frames can't clobber a newer
     * render. Must run on the main thread.
     */
    @MainThread
    fun cancel(appWidgetId: Int) {
        crossfadeAnimators.remove(appWidgetId)?.cancel()
    }

    @MainThread
    fun runCrossfade(
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

        var cancelled = false
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
                        Timber.d(
                            "Crossfade animation cancelled %s",
                            mapOf("appWidgetId" to appWidgetId, "animatedImageViewId" to animatedImageViewId),
                        )

                        cancelled = true
                        // An interrupted fade must never strand the widget on a translucent frame.
                        // Settle on the opaque steady state; the render that cancelled this animation
                        // pushes its own frame right after, showing the same photo, so this is
                        // visually continuous.
                        runCatching { appWidgetManager.updateAppWidget(appWidgetId, finalViews) }
                            .onFailure { Timber.w(it, "Failed to settle cancelled crossfade") }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        Timber.d(
                            "Crossfade animation ended %s",
                            mapOf("appWidgetId" to appWidgetId, "animatedImageViewId" to animatedImageViewId),
                        )

                        if (crossfadeAnimators[appWidgetId] === animation) {
                            crossfadeAnimators.remove(appWidgetId)
                        }
                        if (!cancelled) {
                            // Settle on the durable steady state so a process kill mid-fade is clean.
                            runCatching { appWidgetManager.updateAppWidget(appWidgetId, finalViews) }
                                .onFailure { Timber.w(it, "Failed final crossfade update") }
                        }
                    }
                },
            )
        }

        crossfadeAnimators[appWidgetId] = animator
        animator.start()
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
