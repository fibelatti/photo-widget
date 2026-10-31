package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import timber.log.Timber

class CyclePhotoUseCase @Inject constructor(
    private val widgetProviders: WidgetProviders,
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

        val shuffle: Boolean = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId) && !noShuffle

        // Cycling in order only needs the photos next to the current one, which avoids loading every
        // photo ID. The full list below remains the fallback when the current photo can't be placed.
        if (!shuffle) {
            val currentPhotoId: String? = (currentPhoto ?: photoWidgetStorage.getCurrentPhotoId(appWidgetId))
                ?.ifEmpty { null }
            val adjacentPhotoId: String? = currentPhotoId?.let { photoId: String ->
                photoWidgetStorage.getAdjacentWidgetPhotoId(
                    appWidgetId = appWidgetId,
                    photoId = photoId,
                    forward = direction == Direction.NEXT,
                )
            }

            when {
                adjacentPhotoId != null && adjacentPhotoId == currentPhotoId -> {
                    return showSinglePhoto(appWidgetId = appWidgetId, photoId = adjacentPhotoId)
                }

                adjacentPhotoId != null -> {
                    return showPhoto(appWidgetId = appWidgetId, photoId = adjacentPhotoId, skipSaving = skipSaving)
                }
            }
        }

        val widgetPhotoIds: List<String> = photoWidgetStorage.getSyncedWidgetPhotoIds(appWidgetId = appWidgetId)
            .ifEmpty {
                Timber.w("The widget has no photos to cycle through")
                return ""
            }

        if (widgetPhotoIds.size == 1) {
            return showSinglePhoto(appWidgetId = appWidgetId, photoId = widgetPhotoIds.first())
        }

        // Only shuffling needs to know which photos were already displayed
        val displayedPhotos: MutableList<String> = if (shuffle) {
            photoWidgetStorage.getDisplayedPhotoIds(appWidgetId = appWidgetId).toMutableList()
        } else {
            mutableListOf()
        }

        // Resolve the current photo before any clear. clearDisplayedPhotos() empties the table that
        // backs getCurrentPhotoId(), so reading it afterwards would fall back to the widget index and
        // resolve the wrong photo (which would then be excluded incorrectly below, allowing a repeat).
        val resolvedPhotoId: String? = currentPhoto
            ?: photoWidgetStorage.getCurrentPhotoId(appWidgetId = appWidgetId)
            ?: widgetPhotoIds.getOrNull(photoWidgetStorage.getWidgetIndex(appWidgetId = appWidgetId))
        val currentPhotoId: String? = resolvedPhotoId?.ifEmpty { null }

        // The clear only resets the shuffle "already shown" set. For non-shuffle cycling the next
        // photo is derived from the current index, so clearing here would wipe the current photo
        // tracking and force a fallback to the first photo (visibly skipping a tap).
        var didClear = false
        if (shuffle && direction == Direction.NEXT && displayedPhotos.size >= widgetPhotoIds.size) {
            Timber.i("All photos displayed, starting over")

            if (!skipSaving) {
                photoWidgetStorage.clearDisplayedPhotos(appWidgetId = appWidgetId)
            }

            displayedPhotos.clear()
            didClear = true
        }

        // After a clear, the current photo is no longer in displayedPhotos, so without this it
        // could be picked again immediately, showing the same photo twice in a row.
        if (didClear && currentPhotoId != null) {
            displayedPhotos.add(currentPhotoId)
        }

        val newPhotoId: String = when {
            currentPhotoId == null -> {
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

        return showPhoto(appWidgetId = appWidgetId, photoId = newPhotoId, skipSaving = skipSaving)
    }

    private suspend fun showPhoto(appWidgetId: Int, photoId: String, skipSaving: Boolean): String {
        Timber.i("Updating current photo to $photoId")

        if (!skipSaving) {
            photoWidgetStorage.saveDisplayedPhoto(appWidgetId = appWidgetId, photoId = photoId)
        }

        widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId, allowCrossfade = true)

        return photoId
    }

    private fun showSinglePhoto(appWidgetId: Int, photoId: String): String {
        Timber.i("The widget has a single photo, there is nothing to cycle through")
        // Cannot cycle, but if a photo was recently removed the widget still needs to be updated
        widgetProviders.updatePhotoWidget(appWidgetId = appWidgetId)
        return photoId
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
