package com.fibelatti.photowidget.widget

import android.content.Context
import androidx.work.ListenableWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.RuntimeException
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
        val nextPhotoPath = appWidgetPhotos[nextIndex].path

        photoWidgetStorage.saveWidgetIndex(appWidgetId = appWidgetId, index = nextIndex)

        PhotoWidgetProvider.update(
            context = context,
            appWidgetId = appWidgetId,
            photoPath = nextPhotoPath,
            aspectRatio = photoWidgetStorage.getWidgetAspectRatio(appWidgetId = appWidgetId),
            shapeId = photoWidgetStorage.getWidgetShapeId(appWidgetId = appWidgetId),
            cornerRadius = photoWidgetStorage.getWidgetCornerRadius(appWidgetId = appWidgetId),
            tapAction = photoWidgetStorage.getWidgetTapAction(appWidgetId = appWidgetId),
        )
    }
}
