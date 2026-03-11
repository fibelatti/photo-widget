package com.fibelatti.photowidget.widget

import com.fibelatti.photowidget.widget.PhotoWidgetProvider.Action
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoWidgetProviderActionHandler @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val removeCurrentPhotoUseCase: RemoveCurrentPhotoUseCase,
    private val cyclePhotoUseCase: CyclePhotoUseCase,
    private val photoWidgetStorage: PhotoWidgetStorage,
    private val photoWidgetAlarmManager: PhotoWidgetAlarmManager,
) {

    operator fun invoke(action: Action, appWidgetId: Int, onRemoveActionHandled: (Boolean) -> Unit) {
        coroutineScope.launch {
            if (action == Action.REMOVE_PHOTO) {
                val didRemove: Boolean = removeCurrentPhotoUseCase(appWidgetId = appWidgetId)

                withContext(Dispatchers.Main) {
                    onRemoveActionHandled(didRemove)
                }

                if (!didRemove) return@launch // Exit early since there's nothing else to change
            }

            advanceToNextPhoto(action = action, appWidgetId = appWidgetId)
        }
    }

    private suspend fun advanceToNextPhoto(action: Action, appWidgetId: Int) {
        cyclePhotoUseCase(
            appWidgetId = appWidgetId,
            direction = when (action) {
                Action.VIEW_NEXT_PHOTO, Action.REMOVE_PHOTO -> CyclePhotoUseCase.Direction.NEXT
                Action.VIEW_PREVIOUS_PHOTO -> CyclePhotoUseCase.Direction.PREVIOUS
            },
        )

        photoWidgetStorage.saveWidgetNextCycleTime(appWidgetId = appWidgetId, nextCycleTime = null)
        photoWidgetAlarmManager.setup(appWidgetId = appWidgetId)
    }
}
