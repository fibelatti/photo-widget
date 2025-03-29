package com.fibelatti.photowidget.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import com.google.android.material.color.DynamicColors
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetBorder : Parcelable {

    @get:StringRes
    val label: Int

    val serializedName: String

    fun getBorderPercent(): Float = getBorderWidth() * PERCENT_FACTOR

    private fun getBorderWidth(): Int = when (this) {
        is None -> 0
        is Color -> width
        is Dynamic -> width
        is MatchPhoto -> width
    }

    @Parcelize
    data object None : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_none

        @IgnoredOnParcel
        override val serializedName: String = "NONE"
    }

    @Parcelize
    data class Color(val colorHex: String, val width: Int) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_color

        @IgnoredOnParcel
        override val serializedName: String = "COLOR"
    }

    @Parcelize
    data class Dynamic(val width: Int) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_dynamic

        @IgnoredOnParcel
        override val serializedName: String = "DYNAMIC"
    }

    @Parcelize
    data class MatchPhoto(val width: Int, val type: Type) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_color_wheel

        @IgnoredOnParcel
        override val serializedName: String = "MATCH_PHOTO"

        enum class Type {
            MONOCHROMATIC,
            COMPLEMENTARY,
            ANALOGOUS,
        }
    }

    companion object {

        val VALUE_RANGE: ClosedFloatingPointRange<Float> = 0F..80F

        const val DEFAULT_WIDTH: Int = 40

        /**
         * Calculated based on a 400px image, where 20px was the maximum border width allowed.
         */
        const val PERCENT_FACTOR: Float = .00125F

        val entries: List<PhotoWidgetBorder> by lazy {
            buildList {
                add(None)

                if (DynamicColors.isDynamicColorAvailable()) {
                    add(Dynamic(width = DEFAULT_WIDTH))
                }

                add(MatchPhoto(type = PhotoWidgetBorder.MatchPhoto.Type.MONOCHROMATIC, width = DEFAULT_WIDTH))

                add(Color(colorHex = "ffffff", width = DEFAULT_WIDTH))
            }
        }

        fun fromSerializedName(serializedName: String): PhotoWidgetBorder {
            return entries.firstOrNull { it.serializedName == serializedName } ?: None
        }
    }
}
