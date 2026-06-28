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
        Timber.i(
            "Cycling photo %s",
            mapOf(
                "appWidgetId" to appWidgetId,
                "direction" to direction,
                "noShuffle" to noShuffle,
                "skipSaving" to skipSaving,
                "currentPhoto" to currentPhoto,
            ),
        )

        val widgetPhotoIds: List<String> = photoWidgetStorage.getSyncedWidgetPhotoIds(appWidgetId = appWidgetId)
            .ifEmpty { return "" }

        if (widgetPhotoIds.size == 1) {
            // Cannot cycle, but if a photo was recently removed the widget still needs to be updated
            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
            return widgetPhotoIds.first()
        }

        val displayedPhotos: MutableList<String> = photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId)
            .toMutableList()

        var didClear = false
        if (direction == Direction.NEXT && displayedPhotos.size >= widgetPhotoIds.size) {
            Timber.d("All photos displayed, starting over")

            if (!skipSaving) {
                photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            }

            displayedPhotos.clear()
            didClear = true
        }

        val resolvedPhotoId: String? = currentPhoto
            ?: photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
            ?: widgetPhotoIds.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))
        val currentPhotoId: String? = resolvedPhotoId?.ifEmpty { null }

        val shuffle: Boolean = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId) && !noShuffle

        // After a clear, the current photo is no longer in displayedPhotos, so without this it
        // could be picked again immediately, showing the same photo twice in a row.
        if (didClear && shuffle && currentPhotoId != null) {
            displayedPhotos.add(currentPhotoId)
        }

        val newPhotoId: String = when {
            currentPhotoId == null || (didClear && !shuffle) -> {
                widgetPhotoIds.first()
            }

            shuffle && direction == Direction.PREVIOUS -> {
                if (!skipSaving) {
                    photoWidgetStorage.clearMostRecentPhoto(appWidgetId = appWidgetId)
                }

                previousShufflePhoto(
                    displayedPhotos = displayedPhotos,
                    widgetPhotoIds = widgetPhotoIds,
                    currentPhotoId = currentPhotoId,
                )
            }

            shuffle -> {
                widgetPhotoIds.subtract(displayedPhotos.toSet()).random()
            }

            else -> {
                getNextId(
                    widgetPhotoIds = widgetPhotoIds,
                    currentPhotoId = currentPhotoId,
                    direction = direction,
                )
            }
        }

        Timber.d("Updating current photo to $newPhotoId")

        if (!skipSaving) {
            photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = newPhotoId)
        }

        PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId, allowCrossfade = true)

        return newPhotoId
    }

    private fun previousShufflePhoto(
        displayedPhotos: MutableList<String>,
        widgetPhotoIds: List<String>,
        currentPhotoId: String,
    ): String {
        if (displayedPhotos.isNotEmpty()) {
            // Last index points to the current photo, get it out of the way
            displayedPhotos.removeAt(displayedPhotos.lastIndex)
        }

        return if (displayedPhotos.isNotEmpty()) {
            // Return the previous to last...
            displayedPhotos.removeAt(displayedPhotos.lastIndex)
        } else {
            // `displayedPhotos` contains at most all photos.
            // If we're back at the beginning, just go back in order
            getNextId(widgetPhotoIds = widgetPhotoIds, currentPhotoId = currentPhotoId, direction = Direction.PREVIOUS)
        }
    }

    private fun getNextId(
        widgetPhotoIds: List<String>,
        currentPhotoId: String,
        direction: Direction,
    ): String {
        val currentIndex: Int = widgetPhotoIds.indexOf(currentPhotoId)
        val lastIndex: Int = widgetPhotoIds.lastIndex
        val newIndex: Int = when {
            direction == Direction.PREVIOUS && currentIndex <= 0 -> lastIndex
            direction == Direction.PREVIOUS -> currentIndex - 1
            currentIndex == lastIndex -> 0
            else -> currentIndex + 1
        }

        return widgetPhotoIds.get(index = newIndex)
    }

    enum class Direction {
        NEXT,
        PREVIOUS,
    }
}
