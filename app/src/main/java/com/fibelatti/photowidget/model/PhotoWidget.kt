package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoWidget(
    val source: PhotoWidgetSource = PhotoWidgetSource.PHOTOS,
    val syncedDir: Set<Uri> = emptySet(),
    val photos: List<LocalPhoto> = emptyList(),
    val currentPhoto: LocalPhoto? = null,
    val shuffle: Boolean = false,
    val cycleMode: PhotoWidgetCycleMode = PhotoWidgetCycleMode.DEFAULT,
    val tapAction: PhotoWidgetTapAction = PhotoWidgetTapAction.DEFAULT,
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = DEFAULT_SHAPE_ID,
    val cornerRadius: Float = DEFAULT_CORNER_RADIUS,
    val border: PhotoWidgetBorder = PhotoWidgetBorder.None,
    val opacity: Float = DEFAULT_OPACITY,
    val blackAndWhite: Boolean = false,
    val horizontalOffset: Int = 0,
    val verticalOffset: Int = 0,
    val padding: Int = 0,
    val deletionTimestamp: Long = -1,
    val removedPhotos: List<LocalPhoto> = emptyList(),
    val isLoading: Boolean = false,
) : Parcelable {

    val cyclingEnabled: Boolean
        get() = photos.size > 1 && cycleMode !is PhotoWidgetCycleMode.Disabled

    val canSort: Boolean
        get() = PhotoWidgetSource.PHOTOS == source && photos.size > 1 && !shuffle

    val canShuffle: Boolean
        get() = photos.size > 1

    val increaseBrightness: Boolean
        get() = (tapAction as? PhotoWidgetTapAction.ViewFullScreen)?.increaseBrightness == true

    val viewOriginalPhoto: Boolean
        get() = (tapAction as? PhotoWidgetTapAction.ViewFullScreen)?.viewOriginalPhoto == true

    companion object {

        const val MAX_WIDGET_DIMENSION: Int = 720

        const val DEFAULT_SHAPE_ID = "rounded-square"
        const val DEFAULT_CORNER_RADIUS: Float = 64f
        const val DEFAULT_OPACITY: Float = 100f
    }
}
