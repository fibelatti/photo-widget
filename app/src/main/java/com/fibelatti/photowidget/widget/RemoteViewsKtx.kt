package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.tapActionDisableTap

/**
 * Wires the tap zones shared by [PhotoWidgetProvider] and [TransparentWidgetProvider]. Both
 * layouts expose the same tap-zone ids ([R.id.tap_actions_layout], [R.id.view_tap_left],
 * [R.id.view_tap_center], [R.id.view_tap_right]) so this logic can be reused as-is.
 */
internal fun RemoteViews.setWidgetTapActions(
    context: Context,
    appWidgetId: Int,
    photoWidget: PhotoWidget,
    isLocked: Boolean,
    isCyclePaused: Boolean,
    fillSidesWithCenter: Boolean = false,
) {
    setViewVisibility(R.id.tap_actions_layout, View.VISIBLE)

    val shouldDisableTap: Boolean = photoWidget.tapActionDisableTap && isCyclePaused

    val pendingIntentArgs: (PhotoWidgetTapAction) -> PendingIntent? = { tapAction ->
        TapActionPendingIntentFactory.create(
            context = context,
            appWidgetId = appWidgetId,
            tapAction = tapAction,
            isLocked = isLocked,
            shouldDisableTap = shouldDisableTap,
            originalPhotoPath = photoWidget.currentPhoto?.originalPhotoPath,
            externalUri = photoWidget.currentPhoto?.externalUri,
        )
    }

    val centerPendingIntent: PendingIntent? = pendingIntentArgs(photoWidget.tapActions.center)
    setOnClickPendingIntent(R.id.view_tap_center, centerPendingIntent)

    // A transparent widget is invisible, so users can't tell the tap zones apart. When a side zone
    // has no action of its own, fall back to the center action so the whole surface stays tappable.
    val sideFallback: PendingIntent? = centerPendingIntent.takeIf { fillSidesWithCenter }

    val tapLeftPendingIntent: PendingIntent? = pendingIntentArgs(photoWidget.tapActions.left) ?: sideFallback
    if (tapLeftPendingIntent != null) {
        setViewVisibility(R.id.view_tap_left, View.VISIBLE)
        setOnClickPendingIntent(R.id.view_tap_left, tapLeftPendingIntent)
    } else {
        setViewVisibility(R.id.view_tap_left, View.INVISIBLE)
    }

    val tapRightPendingIntent: PendingIntent? = pendingIntentArgs(photoWidget.tapActions.right) ?: sideFallback
    if (tapRightPendingIntent != null) {
        setViewVisibility(R.id.view_tap_right, View.VISIBLE)
        setOnClickPendingIntent(R.id.view_tap_right, tapRightPendingIntent)
    } else {
        setViewVisibility(R.id.view_tap_right, View.INVISIBLE)
    }
}
