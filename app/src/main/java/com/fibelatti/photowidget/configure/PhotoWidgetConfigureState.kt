package com.fibelatti.photowidget.configure

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio

data class PhotoWidgetConfigureState(
    val photoWidget: PhotoWidget = PhotoWidget(),
    val selectedPhoto: LocalPhoto? = null,
    val isProcessing: Boolean = false,
    val cropQueue: List<LocalPhoto> = emptyList(),
    val messages: List<Message> = emptyList(),
) {

    sealed class Message {

        data object ImportFailed : Message()

        data class LaunchCrop(
            val source: Uri,
            val destination: Uri,
            val aspectRatio: PhotoWidgetAspectRatio,
        ) : Message()

        data class RequestPin(val photoWidget: PhotoWidget) : Message()

        data class AddWidget(val appWidgetId: Int) : Message()

        data object CancelWidget : Message()
    }
}
