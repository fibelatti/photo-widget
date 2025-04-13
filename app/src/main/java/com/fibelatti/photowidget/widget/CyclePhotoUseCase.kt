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

    /**
     * Cycles through the photos in the widget.
     *
     * @param appWidgetId The ID of the widget to update.
     * @param flipBackwards Whether to cycle backwards or forwards.
     * @param noShuffle Whether to disregard the widget shuffle setting.
     * @param skipSaving Whether to skip saving the result of the operation.
     * @param currentPhoto The ID of the current photo being displayed by the widget.
     * @return The ID of the new photo to display.
     */
    suspend operator fun invoke(
        appWidgetId: Int,
        flipBackwards: Boolean = false,
        noShuffle: Boolean = false,
        skipSaving: Boolean = false,
        currentPhoto: String? = null,
    ): String {
        Timber.d(
            "Cycling photo (" +
                "appWidgetId=$appWidgetId," +
                "flipBackwards=$flipBackwards," +
                "noShuffle=$noShuffle," +
                "skipSaving=$skipSaving," +
                "currentPhoto=$currentPhoto" +
                ")",
        )

        val widgetPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
            .first()
            .current
            .map { it.photoId }

        if (widgetPhotos.size < 2) return widgetPhotos.first()

        val displayedPhotos = photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId).toMutableSet()

        if (!flipBackwards && displayedPhotos.size >= widgetPhotos.size && !skipSaving) {
            Timber.d("All photos displayed, starting over")
            photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            displayedPhotos.clear()
        }

        val currentPhotoId: suspend () -> String? = {
            currentPhoto
                ?: photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
                ?: widgetPhotos.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))
        }
        val shuffle: Boolean = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId) && !noShuffle
        val nextRandomPhoto: () -> String = { widgetPhotos.subtract(displayedPhotos).random() }

        val newPhotoId: String = when {
            currentPhotoId() == null -> widgetPhotos.first()

            shuffle && flipBackwards -> {
                if (!skipSaving) {
                    photoWidgetStorage.clearMostRecentPhoto(appWidgetId = appWidgetId)
                }
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

        Timber.d("Updating current photo to $newPhotoId")

        if (!skipSaving) {
            photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = newPhotoId)
            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
        }

        return newPhotoId
    }
}
