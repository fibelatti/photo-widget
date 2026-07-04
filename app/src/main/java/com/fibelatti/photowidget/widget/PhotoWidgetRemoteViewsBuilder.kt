package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.PreparedCurrentPhoto
import com.fibelatti.photowidget.model.textToBitmap
import kotlin.math.abs
import kotlin.math.roundToInt
import timber.log.Timber

/**
 * Builds the [RemoteViews] shown by [PhotoWidgetProvider]. Stateless, so it mirrors the sibling
 * [TapActionPendingIntentFactory] as an `object` rather than an injected collaborator.
 */
object PhotoWidgetRemoteViewsBuilder {

    /**
     * The values that fully describe a single widget render. Identical across the initial and the
     * settle render of a crossfade, so [PhotoWidgetProvider.update] builds it once and reuses it.
     */
    data class RenderState(
        val photoWidget: PhotoWidget,
        val preparedCurrentPhoto: PreparedCurrentPhoto,
        val isLocked: Boolean,
        val isCyclePaused: Boolean,
    )

    /**
     * How the current photo is drawn:
     * - [DEFAULT]: no crossfade. The current photo may render straight from the in-memory bitmap
     *   or from its URI when the one was persisted to stay under IPC transaction limit.
     * - [CROSSFADE_START]: the previous photo is shown opaque and the current photo starts
     *   transparent on top, both from in-memory bitmaps so the host never decodes mid-animation.
     * - [CROSSFADE_SETTLE]: the durable steady state the animation ends on — current photo only,
     *   still from the in-memory bitmap so completing the fade doesn't re-decode and flash.
     */
    enum class RenderMode { DEFAULT, CROSSFADE_START, CROSSFADE_SETTLE }

    /**
     * Returns the (current, previous) image view IDs for the given aspect ratio. The two views
     * share the same scaleType so they can crossfade; `setScaleType` is not a remotable method,
     * hence the dedicated pair per aspect ratio.
     */
    fun imageViewIdsFor(aspectRatio: PhotoWidgetAspectRatio): Pair<Int, Int> {
        return if (aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
            R.id.iv_widget_fill to R.id.iv_widget_fill_prev
        } else {
            R.id.iv_widget to R.id.iv_widget_prev
        }
    }

    fun build(
        context: Context,
        appWidgetId: Int,
        state: RenderState,
        mode: RenderMode,
    ): RemoteViews {
        Timber.d("Building remote views %s", mapOf("mode" to mode))

        val photoWidget: PhotoWidget = state.photoWidget
        val preparedCurrentPhoto: PreparedCurrentPhoto = state.preparedCurrentPhoto
        val (currentImageViewId: Int, previousImageViewId: Int) = imageViewIdsFor(photoWidget.aspectRatio)

        val setupCrossfade: Boolean = mode == RenderMode.CROSSFADE_START
        val renderCurrentFromBitmap: Boolean = mode != RenderMode.DEFAULT

        return RemoteViews(context.packageName, R.layout.photo_widget).apply {
            setViewVisibility(R.id.placeholder_layout, View.GONE)
            setViewVisibility(R.id.iv_widget, View.GONE)
            setViewVisibility(R.id.iv_widget_fill, View.GONE)
            setViewVisibility(R.id.iv_widget_prev, View.GONE)
            setViewVisibility(R.id.iv_widget_fill_prev, View.GONE)

            setViewVisibility(currentImageViewId, View.VISIBLE)
            // The crossfade path forces the in-memory bitmap so the host has no URI to decode
            // mid-animation.
            if (renderCurrentFromBitmap || preparedCurrentPhoto.uri == null) {
                setImageViewBitmap(currentImageViewId, preparedCurrentPhoto.bitmap)
            } else {
                setImageViewUri(currentImageViewId, preparedCurrentPhoto.uri)
            }

            // Restore full opacity on every render except the crossfade start (which deliberately
            // sets TRANSPARENT below). Widget hosts reapply RemoteViews onto the reused ImageView
            // without resetting it, so a fade interrupted before reaching OPAQUE (canceled by a
            // newer update, or with its final frame dropped while the host was suspended), leaves a
            // stuck sub-255 alpha. Setting it here (and in the durable SETTLE views the host caches)
            // heals that translucency the next time any render touches this view.
            if (!setupCrossfade) {
                setInt(
                    currentImageViewId,
                    PhotoWidgetCrossfadeAnimator.METHOD_SET_IMAGE_ALPHA,
                    PhotoWidgetCrossfadeAnimator.OPAQUE,
                )
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
                // (bottom); the animation drives the top view's alpha. See PhotoWidgetCrossfadeAnimator.
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
                isLocked = state.isLocked,
                isCyclePaused = state.isCyclePaused,
            )
        }
    }

    fun buildErrorState(
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

        return RemoteViews(context.packageName, R.layout.photo_widget).apply {
            setViewVisibility(R.id.placeholder_layout, View.VISIBLE)
            setViewVisibility(R.id.iv_widget, View.GONE)
            setViewVisibility(R.id.iv_widget_fill, View.GONE)
            setViewVisibility(R.id.tap_actions_layout, View.GONE)

            setImageViewResource(R.id.iv_placeholder, R.drawable.ic_file_not_found)
            setTextViewText(R.id.tv_placeholder, context.getString(R.string.photo_widget_host_failed))

            setOnClickPendingIntent(R.id.placeholder_layout, pendingIntent)
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
}
