package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import timber.log.Timber

class CyclePhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke(appWidgetId: Int, flipBackwards: Boolean = false) {
        Timber.d("Cycling photo (appWidgetId=$appWidgetId, flipBackwards=$flipBackwards)")

        val widgetPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
            .first()
            .current
            .map { it.photoId }

        if (widgetPhotos.size < 2) return

        val displayedPhotos = photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId).toMutableSet()

        if (!flipBackwards && displayedPhotos.size >= widgetPhotos.size) {
            Timber.d("All photos displayed, starting over")
            photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            displayedPhotos.clear()
        }

        val currentPhotoId: suspend () -> String? = {
            photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
                ?: widgetPhotos.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))
        }
        val shuffle: Boolean = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId)
        val nextRandomPhoto: () -> String = { widgetPhotos.subtract(displayedPhotos).random() }

        val newPhotoId: String = when {
            currentPhotoId() == null -> widgetPhotos.first()

            shuffle && flipBackwards -> {
                photoWidgetStorage.clearMostRecentPhoto(appWidgetId = appWidgetId)
                currentPhotoId() ?: nextRandomPhoto()
            }

            shuffle -> nextRandomPhoto()

            else -> {
                val currentIndex = widgetPhotos.indexOfFirst { it == currentPhotoId() }

                widgetPhotos.get(
                    index = when {
                        flipBackwards && currentIndex == 0 -> widgetPhotos.size - 1
                        flipBackwards -> currentIndex - 1
                        currentIndex == widgetPhotos.size - 1 -> 0
                        else -> currentIndex + 1
                    }.coerceIn(0, widgetPhotos.size - 1),
                )
            }
        }

        photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = newPhotoId)

        Timber.d("Updating current photo to $newPhotoId")

        PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
    }
}
