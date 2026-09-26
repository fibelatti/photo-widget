package com.fibelatti.photowidget.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.ColorPalette
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
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
        override val label = R.string.photo_widget_configure_color_colored

        @IgnoredOnParcel
        override val serializedName: String = "COLOR"
    }

    @Parcelize
    data class Dynamic(
        val width: Int,
        val type: DynamicColorType = DynamicColorType.PRIMARY_INVERSE,
    ) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_color_dynamic

        @IgnoredOnParcel
        override val serializedName: String = "DYNAMIC"
    }

    @Parcelize
    data class MatchPhoto(val width: Int, val type: MatchPhotoColorType) : PhotoWidgetBorder {

        @IgnoredOnParcel
        override val label = R.string.photo_widget_configure_color_palette

        @IgnoredOnParcel
        override val serializedName: String = "MATCH_PHOTO"
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

                add(MatchPhoto(type = MatchPhotoColorType.DOMINANT, width = DEFAULT_WIDTH))

                add(Color(colorHex = "FFFFFF", width = DEFAULT_WIDTH))
            }
        }

        fun fromSerializedName(serializedName: String): PhotoWidgetBorder {
            return entries.firstOrNull { it.serializedName == serializedName } ?: None
        }
    }
}

/**
 * The color used to draw the border. [colorPalette] is only invoked for
 * [PhotoWidgetBorder.MatchPhoto], and must describe the photo the border is drawn around.
 */
@ColorInt
fun PhotoWidgetBorder.resolveColor(context: Context, colorPalette: () -> ColorPalette): Int? = when (this) {
    is PhotoWidgetBorder.None -> null
    is PhotoWidgetBorder.Color -> "#$colorHex".toColorInt()
    is PhotoWidgetBorder.Dynamic -> context.getDynamicAttributeColor(type.colorAttr)
    is PhotoWidgetBorder.MatchPhoto -> colorPalette().colorForType(type)
}

fun PhotoWidgetBorder.borderPercent(): Float = getBorderWidth() * PhotoWidgetBorder.PERCENT_FACTOR

private fun PhotoWidgetBorder.getBorderWidth(): Int = when (this) {
    is PhotoWidgetBorder.None -> 0
    is PhotoWidgetBorder.Color -> width
    is PhotoWidgetBorder.Dynamic -> width
    is PhotoWidgetBorder.MatchPhoto -> width
}
