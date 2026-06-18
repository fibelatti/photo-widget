package com.fibelatti.photowidget.model

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class PhotoWidget(
    val source: PhotoWidgetSource = PhotoWidgetSource.PHOTOS,
    val syncedDir: Set<Uri> = emptySet(),
    val photos: List<LocalPhoto> = emptyList(),
    val currentPhoto: LocalPhoto? = null,
    val shuffle: Boolean = false,
    val directorySorting: DirectorySorting = DirectorySorting.NEWEST_FIRST,
    val cycleMode: PhotoWidgetCycleMode = PhotoWidgetCycleMode.DEFAULT,
    val tapActions: PhotoWidgetTapActions = PhotoWidgetTapActions(),
    val aspectRatio: PhotoWidgetAspectRatio = PhotoWidgetAspectRatio.SQUARE,
    val shapeId: String = DEFAULT_SHAPE_ID,
    val cornerRadius: Int = DEFAULT_CORNER_RADIUS,
    val border: PhotoWidgetBorder = PhotoWidgetBorder.None,
    val colors: PhotoWidgetColors = PhotoWidgetColors(),
    val horizontalOffset: Int = 0,
    val verticalOffset: Int = 0,
    val padding: Int = 0,
    val text: PhotoWidgetText = PhotoWidgetText.None,
    val gifInterval: Long = 0,
    val status: PhotoWidgetStatus = PhotoWidgetStatus.ACTIVE,
    val deletionTimestamp: Long = -1,
    val removedPhotos: List<LocalPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val transparent: Boolean = false,
) : Parcelable {

    companion object {

        const val DEFAULT_SHAPE_ID = "rounded-square"

        /**
         * Default corner radius value in DP. Both the picker and the widget provider will multiply
         * the selected value by the current screen density in order to represent it correctly.
         */
        const val DEFAULT_CORNER_RADIUS: Int = 28

        /**
         * The UI picker offers a [0..100] range which is converted to the corresponding alpha
         * component of [0..255] when preparing the bitmap.
         */
        const val DEFAULT_OPACITY: Float = 100f

        /**
         * The UI picker offers a [-100..100] range which is converted to the persisted range of
         * [0..200] and to the corresponding saturation component of [0..2] when preparing the
         * bitmap. In that range, 1 is identity which is why the default value is 100.
         * The UI takes care of subtracting/adding as needed.
         */
        const val DEFAULT_SATURATION: Float = 100f

        /**
         * The UI picker offers a [-100..100] range which is converted to the corresponding
         * brightness component of [-255..255] when preparing the bitmap.
         */
        const val DEFAULT_BRIGHTNESS: Float = 0f

        /**
         * Padding and Offset values are in DP. Both the picker and the widget provider will
         * multiply their set value by the current screen density in order to have it represented
         * correctly, times this constant to make each step more meaningful. Using base 4 to match
         * the Android grid.
         */
        const val POSITIONING_MULTIPLIER: Int = 4

        fun isDraftWidgetId(widgetId: Int): Boolean = widgetId < 0
    }
}

// region ktx

val PhotoWidget.photoCycleEnabled: Boolean
    get() = photos.size > 1 && cycleMode !is PhotoWidgetCycleMode.Disabled

val PhotoWidget.canSort: Boolean
    get() = source == PhotoWidgetSource.PHOTOS &&
        photos.size > 1 &&
        !shuffle &&
        cycleMode !is PhotoWidgetCycleMode.AdvancedSchedule

val PhotoWidget.canShuffle: Boolean
    get() = source != PhotoWidgetSource.GIF &&
        photos.size > 1 &&
        cycleMode !is PhotoWidgetCycleMode.AdvancedSchedule

val PhotoWidget.canSync: Boolean
    get() = source == PhotoWidgetSource.DIRECTORY

val PhotoWidget.canLock: Boolean
    get() = photos.isNotEmpty() && source != PhotoWidgetSource.GIF

val PhotoWidget.tapActionIncreaseBrightness: Boolean
    get() = tapActions.increaseBrightness

val PhotoWidget.tapActionViewOriginalPhoto: Boolean
    get() = tapActions.viewOriginalPhoto

val PhotoWidget.tapActionNoShuffle: Boolean
    get() = tapActions.noShuffle

val PhotoWidget.tapActionKeepCurrentPhoto: Boolean
    get() = tapActions.keepCurrentPhoto

val PhotoWidget.tapActionDisableTap: Boolean
    get() = tapActions.disableTap

val PhotoWidget.tapActionViewerBackgroundColorHex: String?
    get() = tapActions.viewerBackgroundColorHex

fun PhotoWidget.orderedPhotosForDisplay(): List<LocalPhoto> {
    if (cycleMode !is PhotoWidgetCycleMode.AdvancedSchedule) return photos

    val schedule: Map<String, Time> = cycleMode.schedule
    val timed: List<LocalPhoto> = photos
        .filter { it.photoId in schedule }
        .sortedWith(compareBy({ schedule.getValue(it.photoId).hour }, { schedule.getValue(it.photoId).minute }))
    val untimed: List<LocalPhoto> = photos.filter { it.photoId !in schedule }
    return timed + untimed
}

// endregion ktx
