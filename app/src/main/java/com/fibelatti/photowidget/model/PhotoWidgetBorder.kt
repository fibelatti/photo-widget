package com.fibelatti.photowidget.model

import android.os.Parcelable
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import com.fibelatti.photowidget.R
import com.google.android.material.color.DynamicColors
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetBorder : Parcelable {

    @get:StringRes
    val label: Int

    val serializedName: String

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
    data class Dynamic(val width: Int, val type: Type = Type.PRIMARY_INVERSE) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_dynamic

        @IgnoredOnParcel
        override val serializedName: String = "DYNAMIC"

        enum class Type(@AttrRes val colorAttr: Int, @StringRes val label: Int) {
            PRIMARY_INVERSE(
                colorAttr = com.google.android.material.R.attr.colorPrimaryInverse,
                label = R.string.photo_widget_configure_border_dynamic_inverse,
            ),
            PRIMARY(
                colorAttr = androidx.appcompat.R.attr.colorPrimary,
                label = R.string.photo_widget_configure_border_dynamic_primary,
            ),
            PRIMARY_FIXED(
                colorAttr = com.google.android.material.R.attr.colorPrimaryFixed,
                label = R.string.photo_widget_configure_border_dynamic_primary_fixed,
            ),
            SECONDARY(
                colorAttr = com.google.android.material.R.attr.colorSecondary,
                label = R.string.photo_widget_configure_border_dynamic_secondary,
            ),
            SECONDARY_FIXED(
                colorAttr = com.google.android.material.R.attr.colorSecondaryFixed,
                label = R.string.photo_widget_configure_border_dynamic_secondary_fixed,
            ),
        }
    }

    @Parcelize
    data class MatchPhoto(val width: Int, val type: Type) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_border_color_palette

        @IgnoredOnParcel
        override val serializedName: String = "MATCH_PHOTO"

        enum class Type(@StringRes val label: Int) {
            DOMINANT(R.string.photo_widget_configure_border_color_palette_dominant),
            VIBRANT(R.string.photo_widget_configure_border_color_palette_vibrant),
            MUTED(R.string.photo_widget_configure_border_color_palette_muted),
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

                add(MatchPhoto(type = MatchPhoto.Type.DOMINANT, width = DEFAULT_WIDTH))

                add(Color(colorHex = "FFFFFF", width = DEFAULT_WIDTH))
            }
        }

        fun fromSerializedName(serializedName: String): PhotoWidgetBorder {
            return entries.firstOrNull { it.serializedName == serializedName } ?: None
        }
    }
}

fun PhotoWidgetBorder.borderPercent(): Float = getBorderWidth() * PhotoWidgetBorder.PERCENT_FACTOR

private fun PhotoWidgetBorder.getBorderWidth(): Int = when (this) {
    is PhotoWidgetBorder.None -> 0
    is PhotoWidgetBorder.Color -> width
    is PhotoWidgetBorder.Dynamic -> width
    is PhotoWidgetBorder.MatchPhoto -> width
}
