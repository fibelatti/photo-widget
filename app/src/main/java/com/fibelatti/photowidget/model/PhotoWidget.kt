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
    val status: PhotoWidgetStatus = PhotoWidgetStatus.ACTIVE,
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

    val tapActionIncreaseBrightness: Boolean
        get() = tapActions.increaseBrightness

    val tapActionViewOriginalPhoto: Boolean
        get() = tapActions.viewOriginalPhoto

    val tapActionNoShuffle: Boolean
        get() = tapActions.noShuffle

    val tapActionKeepCurrentPhoto: Boolean
        get() = tapActions.keepCurrentPhoto

    val tapActionDisableTap: Boolean
        get() = tapActions.disableTap

    companion object {

        /**
         * This dimension establishes a safe constant for the shapes library. Certain shapes can
         * throw an exception when larger than this.
         */
        const val MAX_WIDGET_DIMENSION: Int = 700

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
    }
}
