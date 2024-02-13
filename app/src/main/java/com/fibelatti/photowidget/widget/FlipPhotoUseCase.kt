package com.fibelatti.photowidget.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FlipPhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    operator fun invoke(appWidgetId: Int) {
        val appWidgetPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)

        if (appWidgetPhotos.size < 2) return

        val currentIndex = photoWidgetStorage.getWidgetIndex(appWidgetId)
        val nextIndex = if (currentIndex == appWidgetPhotos.size - 1) 0 else currentIndex + 1

        photoWidgetStorage.saveWidgetIndex(appWidgetId = appWidgetId, index = nextIndex)

        PhotoWidgetProvider.update(
            context = context,
            appWidgetId = appWidgetId,
        )
    }
}
