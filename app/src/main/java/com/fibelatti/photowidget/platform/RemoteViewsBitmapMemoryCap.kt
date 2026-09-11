package com.fibelatti.photowidget.platform

/**
 * The maximum [bytes] of bitmap a `RemoteViews` update may carry, as reported by the widget host,
 * alongside the count of pixels of the display the limit was reported for.
 */
data class RemoteViewsBitmapMemoryCap(
    val bytes: Long,
    val displayPixels: Long,
)
