package com.fibelatti.photowidget.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class FlipPhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val coroutineScope: CoroutineScope,
) {

    operator fun invoke(appWidgetId: Int) {
        coroutineScope.launch {
            val appWidgetPhotos = photoWidgetStorage.getWidgetPhotos(appWidgetId = appWidgetId)

            if (appWidgetPhotos.size < 2) return@launch

            val currentIndex = photoWidgetStorage.getWidgetIndex(appWidgetId)
            val shuffle = photoWidgetStorage.getWidgetShuffle(appWidgetId = appWidgetId)
            val nextIndex = when {
                shuffle -> {
                    val indices = appWidgetPhotos.indices
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

                currentIndex == appWidgetPhotos.size - 1 -> 0

                else -> currentIndex + 1
            }

            photoWidgetStorage.saveWidgetIndex(appWidgetId = appWidgetId, index = nextIndex)

            PhotoWidgetProvider.update(context = context, appWidgetId = appWidgetId)
        }
    }
}
