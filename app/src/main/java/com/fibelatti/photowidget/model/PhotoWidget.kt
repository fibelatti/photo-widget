package com.fibelatti.photowidget.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidget(
    val photos: List<LocalPhoto> = emptyList(),
    val currentIndex: Int = 0,
    val loopingInterval: PhotoWidgetLoopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
    val tapAction: PhotoWidgetTapAction = PhotoWidgetTapAction.NONE,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
    val cornerRadius: Float = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
) : Parcelable {

    val currentPhoto: LocalPhoto get() = photos[currentIndex]

    val loopingEnabled: Boolean get() = photos.size > 1

    val order: List<String> get() = photos.map { it.name }
}
