package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class CyclePhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(appWidgetId: Int, flipBackwards: Boolean = false) {
        Timber.d("Cycling photo (appWidgetId=$appWidgetId, flipBackwards=$flipBackwards)")

        val widgetPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId, loadFromSource = false)
            .current
            .map { it.photoId }

        if (widgetPhotos.size < 2) return

        val displayedPhotos = photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId).toMutableSet()

        if (displayedPhotos.size >= widgetPhotos.size) {
            Timber.d("All photos displayed, starting over")
            photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            displayedPhotos.clear()
        }

        val currentPhotoId = photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
            ?: widgetPhotos.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))
        val currentIndex = widgetPhotos.indexOfFirst { it == currentPhotoId }
        val shuffle = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId)

        val newPhotoId = when {
            currentPhotoId == null -> widgetPhotos.first()
            shuffle -> widgetPhotos.subtract(displayedPhotos).random()
            else -> widgetPhotos.get(
                index = when {
                    flipBackwards && currentIndex == 0 -> widgetPhotos.size - 1
                    flipBackwards -> currentIndex - 1
                    currentIndex == widgetPhotos.size - 1 -> 0
                    else -> currentIndex + 1
                }.coerceIn(0, widgetPhotos.size - 1),
            )
        }

        photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = newPhotoId)

        Timber.d("Updating current photo from $currentPhotoId to $newPhotoId")

        PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
    }
}
