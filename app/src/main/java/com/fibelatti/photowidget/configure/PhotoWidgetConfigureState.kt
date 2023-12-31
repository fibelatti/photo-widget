package com.fibelatti.photowidget.configure

import android.net.Uri
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder

data class PhotoWidgetConfigureState(
    val photos: List<String> = emptyList(),
    val loopingInterval: PhotoWidgetLoopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = PhotoWidgetShapeBuilder.defaultShapeId(),
    val message: Message? = null,
) {

    sealed class Message {

        data class LaunchCrop(
            val source: Uri,
            val destination: Uri,
            val aspectRatio: PhotoWidgetAspectRatio,
        ) : Message()

        data class RequestPin(
            val photoPath: String,
            val enableLooping: Boolean,
            val loopingInterval: PhotoWidgetLoopingInterval,
            val aspectRatio: PhotoWidgetAspectRatio,
            val shapeId: String,
        ) : Message()

        data class AddWidget(
            val appWidgetId: Int,
            val photoPath: String,
            val aspectRatio: PhotoWidgetAspectRatio,
            val shapeId: String,
        ) : Message()

        data object CancelWidget : Message()
    }
}
