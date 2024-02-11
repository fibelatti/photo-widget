package com.fibelatti.photowidget.configure

import android.net.Uri
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetTapAction

data class PhotoWidgetConfigureState(
    val photos: List<LocalPhoto> = emptyList(),
    val selectedPhoto: LocalPhoto? = null,
    val loopingInterval: PhotoWidgetLoopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
    val tapAction: PhotoWidgetTapAction = PhotoWidgetTapAction.VIEW_FULL_SCREEN,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = PhotoWidgetShapeBuilder.defaultShapeId(),
    val cornerRadius: Float = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
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

        data class RequestPin(
            val photoPath: String,
            val order: List<String>,
            val enableLooping: Boolean,
            val loopingInterval: PhotoWidgetLoopingInterval,
            val tapAction: PhotoWidgetTapAction,
            val aspectRatio: PhotoWidgetAspectRatio,
            val shapeId: String,
            val cornerRadius: Float,
        ) : Message()

        data class AddWidget(
            val appWidgetId: Int,
            val photoPath: String,
            val aspectRatio: PhotoWidgetAspectRatio,
            val shapeId: String,
            val cornerRadius: Float,
            val tapAction: PhotoWidgetTapAction,
        ) : Message()

        data object CancelWidget : Message()
    }
}
