package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeleteStaleDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    suspend operator fun invoke() {
        val existingWidgetIds: List<Int> = PhotoWidgetProvider.ids(context = context) +
            TransparentWidgetProvider.ids(context = context)
        photoWidgetStorage.deleteUnusedWidgetData(existingWidgetIds = existingWidgetIds)
    }
}
