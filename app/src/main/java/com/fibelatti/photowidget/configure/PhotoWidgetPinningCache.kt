package com.fibelatti.photowidget.configure

import com.fibelatti.photowidget.model.PhotoWidget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class keeps temporary widget data available in-memory. The data is available between a pin
 * request and its confirmation callback being received.
 *
 * It is used to workaround two scenarios:
 *
 * - `AppWidgetManager#requestPinAppWidget` can fail due to the transaction being too large if the
 * [widget data][PhotoWidget] is added to the bundle of the success callback intent. The extras are
 * also not delivered on Samsung devices, so instead they are shared using this class.
 *
 * - `AppWidgetProvider#onUpdate` is called with the new widget ID as the user starts dragging the
 * widget from the pinning dialog to their home screen, but `PhotoWidgetPinnedReceiver` wouldn't
 * have been called yet to move the content to the directory that corresponds to that ID.
 */
@Singleton
class PhotoWidgetPinningCache @Inject constructor() {

    /**
     * Returns the [PhotoWidget] that's currently being pinned.
     */
    var pendingWidget: PhotoWidget? = null
        private set

    /**
     * Returns the draft ID of the widget being pinned, so its data can be migrated to the real ID.
     */
    var pendingDraftId: Int? = null
        private set

    /**
     * Sets the data corresponding to the widget that's being pinned.
     */
    fun populate(pendingWidget: PhotoWidget, draftWidgetId: Int) {
        this.pendingWidget = pendingWidget
        this.pendingDraftId = draftWidgetId
    }

    /**
     * Gets the data for the widget being pinned and clears the cache.
     *
     * @return A pair of the [PhotoWidget] and the draft widget ID, or null if no data is cached.
     */
    fun consume(): Pair<PhotoWidget, Int>? {
        val widget = pendingWidget ?: return null
        val draftId = pendingDraftId ?: return null
        pendingWidget = null
        pendingDraftId = null
        return widget to draftId
    }
}
