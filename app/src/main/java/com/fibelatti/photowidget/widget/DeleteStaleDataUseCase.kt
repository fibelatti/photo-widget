package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject

class DeleteStaleDataUseCase @Inject constructor(
    private val widgetProviders: WidgetProviders,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke() {
        val existingWidgetIds: List<Int> = widgetProviders.photoWidgetIds() +
            widgetProviders.transparentWidgetIds()
        photoWidgetStorage.deleteUnusedWidgetData(existingWidgetIds = existingWidgetIds)
        photoWidgetStorage.releaseUnusedUriPermissions(existingWidgetIds = existingWidgetIds)
    }
}
