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
     * @param direction Whether to view the next or previous photo.
     * @param noShuffle Whether to disregard the widget shuffle setting.
     * @param skipSaving Whether to skip saving the result of the operation.
     * @param currentPhoto The ID of the current photo being displayed by the widget.
     * @return The ID of the new photo to display.
     */
    suspend operator fun invoke(
        appWidgetId: Int,
        direction: Direction = Direction.NEXT,
        noShuffle: Boolean = false,
        skipSaving: Boolean = false,
        currentPhoto: String? = null,
    ): String {
        Timber.d(
            "Cycling photo (" +
                "appWidgetId=$appWidgetId," +
                "direction=$direction," +
                "noShuffle=$noShuffle," +
                "skipSaving=$skipSaving," +
                "currentPhoto=$currentPhoto" +
                ")",
        )

        val widgetPhotoIds: List<String> = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)
            .first()
            .current
            .map { it.photoId }

        when {
            widgetPhotoIds.isEmpty() -> return ""
            widgetPhotoIds.size == 1 -> return widgetPhotoIds.first()
        }

        val displayedPhotos = photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId).toMutableSet()

        var didClear = false
        if (Direction.NEXT == direction && displayedPhotos.size >= widgetPhotoIds.size && !skipSaving) {
            Timber.d("All photos displayed, starting over")
            photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            displayedPhotos.clear()
            didClear = true
        }

        val currentPhotoId: suspend () -> String? = {
            val resolved: String? = currentPhoto
                ?: photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
                ?: widgetPhotoIds.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))

            resolved?.ifEmpty { null }
        }
        val shuffle: Boolean = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId) && !noShuffle
        val nextRandomPhoto: () -> String = { widgetPhotoIds.subtract(displayedPhotos).random() }

        val newPhotoId: String = when {
            didClear || currentPhotoId() == null -> widgetPhotoIds.first()

            shuffle && Direction.PREVIOUS == direction -> {
                if (!skipSaving) {
                    photoWidgetStorage.clearMostRecentPhoto(appWidgetId = appWidgetId)
                }
                currentPhotoId() ?: nextRandomPhoto()
            }

            shuffle -> nextRandomPhoto()

            else -> {
                val currentIndex: Int = widgetPhotoIds.indexOfFirst { it == currentPhotoId() }
                val newIndex: Int = when {
                    Direction.PREVIOUS == direction && currentIndex <= 0 -> widgetPhotoIds.size - 1
                    Direction.PREVIOUS == direction -> currentIndex - 1
                    currentIndex == widgetPhotoIds.size - 1 -> 0
                    else -> currentIndex + 1
                }.coerceIn(0, widgetPhotoIds.size - 1)

                widgetPhotoIds.get(index = newIndex)
            }
        }

        Timber.d("Updating current photo to $newPhotoId")

        if (!skipSaving) {
            photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = newPhotoId)
            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
        }

        return newPhotoId
    }

    enum class Direction {
        NEXT,
        PREVIOUS,
    }
}
