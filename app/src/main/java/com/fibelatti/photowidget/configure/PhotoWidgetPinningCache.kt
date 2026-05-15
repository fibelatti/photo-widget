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
     * Returns the cached data without clearing it. The cache must remain populated until the data
     * has been persisted under the real widget ID, otherwise a racy `AppWidgetProvider#onUpdate`
     * may trigger `LoadPhotoWidgetUseCase` and write rows keyed by the new ID, which then collide
     * with the migration performed by [PhotoWidgetPinnedReceiver].
     */
    fun peek(): Pair<PhotoWidget, Int>? {
        val widget = pendingWidget ?: return null
        val draftId = pendingDraftId ?: return null
        return widget to draftId
    }

    /**
     * Clears the cache. Call this only after the pinned widget data has been migrated.
     */
    fun clear() {
        pendingWidget = null
        pendingDraftId = null
    }
}
