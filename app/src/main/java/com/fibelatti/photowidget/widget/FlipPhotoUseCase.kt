package com.fibelatti.photowidget.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FlipPhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val coroutineScope: CoroutineScope,
) {

    operator fun invoke(appWidgetId: Int) {
        coroutineScope.launch {
            Timber.d("Flipping widget (appWidgetId=$appWidgetId)")

            val count = photoWidgetStorage.getWidgetPhotoCount(appWidgetId = appWidgetId)

            if (count < 2) return@launch

            val currentIndex = photoWidgetStorage.getWidgetIndex(appWidgetId)
            val shuffle = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId)
            val nextIndex = when {
                shuffle -> {
                    val indices = 0..count
                    val pastIndices = photoWidgetStorage.getWidgetPastIndices(appWidgetId = appWidgetId)
                    val remaining = indices - pastIndices

                    if (remaining.isEmpty()) {
                        indices.minus(currentIndex).random().also {
                            photoWidgetStorage.saveWidgetPastIndices(
                                appWidgetId = appWidgetId,
                                pastIndices = setOf(it),
                            )
                        }
                    } else {
                        remaining.random().also {
                            photoWidgetStorage.saveWidgetPastIndices(
                                appWidgetId = appWidgetId,
                                pastIndices = pastIndices + it,
                            )
                        }
                    }
                }

                currentIndex == count - 1 -> 0

                else -> currentIndex + 1
            }

            Timber.d("Updating index from $currentIndex to $nextIndex")
            photoWidgetStorage.saveWidgetIndex(appWidgetId = appWidgetId, index = nextIndex)

            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
        }
    }
}
