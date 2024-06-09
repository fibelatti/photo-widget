package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidget(
    val source: PhotoWidgetSource = PhotoWidgetSource.PHOTOS,
    val syncedDir: Set<Uri> = emptySet(),
    val photos: List<LocalPhoto> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val loopingInterval: PhotoWidgetLoopingInterval = PhotoWidgetLoopingInterval.ONE_DAY,
    val intervalBasedLoopingEnabled: Boolean = true,
    val tapAction: PhotoWidgetTapAction = PhotoWidgetTapAction.NONE,
    val increaseBrightness: Boolean = false,
    val appShortcut: String? = null,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID,
    val cornerRadius: Float = PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
    val opacity: Float = DEFAULT_OPACITY,
) : Parcelable {

    val currentPhoto: LocalPhoto
        get() = try {
            photos[currentIndex]
        } catch (_: Exception) {
            photos.first()
        }

    val loopingEnabled: Boolean get() = photos.size > 1 && intervalBasedLoopingEnabled

    val order: List<String> get() = photos.map { it.name }

    val canSort: Boolean get() = PhotoWidgetSource.PHOTOS == source && photos.size > 1 && !shuffle

    val canShuffle: Boolean
        get() = photos.size > 1 && (intervalBasedLoopingEnabled || PhotoWidgetTapAction.VIEW_NEXT_PHOTO == tapAction)

    companion object {

        const val MAX_WIDGET_DIMENSION: Int = 720
        const val MAX_STORAGE_DIMENSION: Int = 1_920

        const val DEFAULT_OPACITY: Float = 100f
    }
}
