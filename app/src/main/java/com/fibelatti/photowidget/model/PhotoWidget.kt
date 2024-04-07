package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidget(
    val source: PhotoWidgetSource = PhotoWidgetSource.PHOTOS,
    val syncedDir: Uri? = null,
    val photos: List<LocalPhoto> = emptyList(),
    val currentIndex: Int = 0,
    val loopingInterval: PhotoWidgetLoopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
    val intervalBasedLoopingEnabled: Boolean = true,
    val tapAction: PhotoWidgetTapAction = PhotoWidgetTapAction.NONE,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
    val cornerRadius: Float = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
) : Parcelable {

    val currentPhoto: LocalPhoto get() = photos[currentIndex]

    val loopingEnabled: Boolean get() = photos.size > 1 && intervalBasedLoopingEnabled

    val order: List<String> get() = photos.map { it.name }
}
