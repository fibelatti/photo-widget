package com.fibelatti.photowidget.configure

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio

data class PhotoWidgetConfigureState(
    val photoWidget: PhotoWidget = PhotoWidget(),
    val selectedPhoto: LocalPhoto? = null,
    val isProcessing: Boolean = true,
    val cropQueue: List<LocalPhoto> = emptyList(),
    val messages: List<Message> = emptyList(),
    val hasEdits: Boolean = false,
) {

    sealed class Message {

        data object SuggestImport : Message()

        data object ImportFailed : Message()

        data object TooManyPhotos : Message()

        data class LaunchCrop(
            val source: Uri,
            val destination: Uri,
            val aspectRatio: PhotoWidgetAspectRatio,
        ) : Message()

        data object RequestPin : Message()

        data class AddWidget(val appWidgetId: Int) : Message()

        data object MissingPhotos : Message()

        data object CancelWidget : Message()
    }
}
