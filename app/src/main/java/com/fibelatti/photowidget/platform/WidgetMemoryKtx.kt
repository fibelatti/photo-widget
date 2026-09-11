package com.fibelatti.photowidget.platform

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt
import timber.log.Timber

/**
 * Pixel count of the display the app is laid out for.
 */
fun Context.displayPixels(): Long {
    val displayMetrics: DisplayMetrics = resources.displayMetrics
    return displayMetrics.heightPixels.toLong() * displayMetrics.widthPixels
}

/**
 * The maximum bytes of bitmap a single `RemoteViews` update may carry before the widget host
 * rejects it.
 *
 * The host enforces `6 * the real size of the default display`, measured once when the system
 * started and never exposed to apps. [DisplayMetrics] are used to calculate an educated guess
 * for the display the app is currently laid out for. A rejected update reports the host's
 * actual value.
 *
 * @See UserPreferencesStorage.remoteViewsBitmapMemoryCap
 */
private fun Context.getMaxRemoteViewsBitmapMemory(): Long {
    val displayPixels: Long = displayPixels()
    val estimate: Long = (displayPixels * 4 * 1.5).toLong()
    val reportedByHost: RemoteViewsBitmapMemoryCap? = hostReportedBitmapMemoryCap()
        ?.takeIf { cap -> cap.displayPixels == displayPixels }

    return if (reportedByHost != null) minOf(estimate, reportedByHost.bytes) else estimate
}

/**
 * Bytes available to the photo of a single widget render, after reserving [labelBytes] for the
 * label bitmap traveling in the same update.
 *
 * Only a fraction of the budget is offered because the budget itself may be wrong as the display
 * figure it starts from can exceed the host's real cap. Each [recoveryAttempt] after a rejected
 * render takes a smaller share, so a widget that keeps being rejected converges on something the
 * host accepts instead of failing the same way twice.
 */
private fun Context.getMaxWidgetPhotoMemory(recoveryAttempt: Int = 0, labelBytes: Long = 0): Long {
    val fraction: Double = if (recoveryAttempt <= 0) {
        SINGLE_RENDER_BUDGET_FRACTION
    } else {
        RECOVERY_BUDGET_FRACTIONS[(recoveryAttempt - 1).coerceAtMost(RECOVERY_BUDGET_FRACTIONS.lastIndex)]
    }

    return ((getMaxRemoteViewsBitmapMemory() * fraction).toLong() - labelBytes)
        .coerceAtLeast(MIN_PHOTO_MEMORY)
}

/**
 * Bytes available to the two bitmaps a crossfade carries together (current + previous), the label
 * bitmap included. Both the sizing of the pair ([getMaxCrossfadeBitmapDimension]) and the check
 * that the pair fits ([PhotoWidgetProvider]) measure against this same value.
 */
fun Context.getMaxCrossfadeBitmapMemory(): Long {
    return (getMaxRemoteViewsBitmapMemory() * CROSSFADE_BUDGET_FRACTION).toLong()
}

/**
 * Max size in pixels for the photo of a single widget render of [appWidgetId].
 *
 * [coerceToWidgetSize] must be false for a photo the widget crops to fill itself, which can need
 * more pixels than the widget measures: see [Context.coerceToWidgetSize].
 */
fun Context.getMaxWidgetPhotoDimension(
    appWidgetId: Int,
    recoveryAttempt: Int = 0,
    labelBytes: Long = 0,
    coerceToWidgetSize: Boolean = true,
): Int {
    val maxMemory: Long = getMaxWidgetPhotoMemory(recoveryAttempt = recoveryAttempt, labelBytes = labelBytes)
    val maxMemoryDimension: Int = memoryToMaxDimension(maxMemory)
    val maxDimension: Int = if (coerceToWidgetSize) {
        coerceToWidgetSize(appWidgetId = appWidgetId, dimension = maxMemoryDimension)
    } else {
        maxMemoryDimension
    }

    Timber.d(
        "Max widget photo dimension: $maxDimension %s",
        mapOf(
            "maxMemory" to maxMemory,
            "recoveryAttempt" to recoveryAttempt,
            "labelBytes" to labelBytes,
        ),
    )

    return maxDimension
}

/**
 * Max size in pixels for each of the two bitmaps carried together in a crossfade update of
 * [appWidgetId], sharing [getMaxCrossfadeBitmapMemory] between them.
 */
fun Context.getMaxCrossfadeBitmapDimension(
    appWidgetId: Int,
    labelBytes: Long = 0,
    coerceToWidgetSize: Boolean = true,
): Int {
    val perBitmapMemory: Long = ((getMaxCrossfadeBitmapMemory() - labelBytes) / 2)
        .coerceAtLeast(MIN_PHOTO_MEMORY)
    val maxDimension: Int = memoryToMaxDimension(perBitmapMemory)

    return if (coerceToWidgetSize) {
        coerceToWidgetSize(appWidgetId = appWidgetId, dimension = maxDimension)
    } else {
        maxDimension
    }
}

/**
 * Caps [dimension] at the largest side the widget can actually display, so a photo is never
 * decoded larger than the widget it is drawn into. Hosts that report nothing usable leave
 * [dimension] as it is.
 */
private fun Context.coerceToWidgetSize(appWidgetId: Int, dimension: Int): Int {
    val options: Bundle = runCatching {
        AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId)
    }.getOrNull() ?: return dimension

    // The four bounds are the box enclosing every size the host may display the widget at, across
    // orientations and, on a folding device, postures. Which of those sizes is on screen right now
    // is not recoverable from them: a device offering several reports bounds that belong to
    // different sizes, so no pairing of them describes a widget that exists. Only the upper bounds
    // are of use here, as the largest side of the box cannot be smaller than the largest side of
    // the widget, which is all a cap needs. A widget currently smaller than that decodes a photo
    // larger than it displays.
    val widthDp: Int = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    val heightDp: Int = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    val largestSideDp: Int = maxOf(widthDp, heightDp)

    Timber.d(
        "Widget size reported by the host %s",
        mapOf("appWidgetId" to appWidgetId, "widthDp" to widthDp, "heightDp" to heightDp),
    )

    if (largestSideDp <= 0) return dimension

    return minOf(dimension, (largestSideDp * resources.displayMetrics.density).roundToInt())
}

/**
 * The host's own cap, learned from an update it rejected, or null while none has been rejected on
 * this device.
 */
private fun Context.hostReportedBitmapMemoryCap(): RemoteViewsBitmapMemoryCap? = runCatching {
    entryPoint<PhotoWidgetEntryPoint>(this).userPreferencesStorage().remoteViewsBitmapMemoryCap
}.getOrNull()

/**
 * Largest side in pixels of a square bitmap fitting in [memory], rounded down so it doesn't
 * extrapolate the actual limit.
 */
private fun memoryToMaxDimension(memory: Long): Int {
    return floor(sqrt(memory / BYTES_PER_PIXEL.toDouble())).toInt().coerceAtLeast(1)
}

/**
 * Max size in pixels for photos decoded to be displayed in the app itself, where the `RemoteViews`
 * bitmap cap doesn't apply and the display is the only constraint that matters.
 * Widget renders are sized by [getMaxWidgetPhotoDimension] instead.
 */
fun Context.getMaxBitmapWidgetDimension(coerceMaxMemory: Boolean = false): Int {
    Timber.d("Calculating max dimension %s", mapOf("coerceMaxMemory" to coerceMaxMemory))

    val displayMetrics: DisplayMetrics = resources.displayMetrics
    val maxMemoryAllowed: Long = if (coerceMaxMemory) {
        IN_APP_CONSTRAINED_PHOTO_MEMORY
    } else {
        getMaxRemoteViewsBitmapMemory()
    }
    val maxDimension: Int = memoryToMaxDimension((maxMemoryAllowed / displayMetrics.density).toLong())

    Timber.d("Max dimension allowed: $maxDimension %s", mapOf("maxMemoryAllowed" to maxMemoryAllowed))

    return maxDimension
}

/**
 * Share of the budget a plain render offers the photo.
 */
private const val SINGLE_RENDER_BUDGET_FRACTION: Double = 0.5

/**
 * Share of the budget a crossfade offers its two bitmaps and the label together.
 */
private const val CROSSFADE_BUDGET_FRACTION: Double = 0.45

/**
 * Shares offered to the successive renders that follow a rejected one, each well under any cap a
 * host is likely to enforce.
 */
private val RECOVERY_BUDGET_FRACTIONS: List<Double> = listOf(0.25, 0.125)

/**
 * Ceiling for photos decoded by the parts of the app that ask to be memory constrained, such as
 * the widget previews rendered while configuring one. Unrelated to what a widget host accepts.
 */
private const val IN_APP_CONSTRAINED_PHOTO_MEMORY: Long = 6_912_000

private const val BYTES_PER_PIXEL: Int = 4

/**
 * Floor for any photo budget, so a small or mis-learned cap still renders a recognizable photo
 * rather than a few pixels.
 */
private const val MIN_PHOTO_MEMORY: Long = 256L * 256 * BYTES_PER_PIXEL
