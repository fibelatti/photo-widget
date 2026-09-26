package com.fibelatti.photowidget.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetTextColor.Companion.fromSerialized
import com.fibelatti.photowidget.platform.ColorPalette
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.google.android.material.color.DynamicColors
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetTextColor : Parcelable {

    @get:StringRes
    val label: Int

    val serializedName: String

    /** The value that, together with [serializedName], restores this color with [fromSerialized]. */
    val serializedValue: String

    @Parcelize
    data class Colored(val colorHex: String) : PhotoWidgetTextColor {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_color_colored

        @IgnoredOnParcel
        override val serializedName: String = "COLORED"

        @IgnoredOnParcel
        override val serializedValue: String = colorHex
    }

    @Parcelize
    data class Dynamic(
        val type: DynamicColorType = DynamicColorType.PRIMARY_INVERSE,
    ) : PhotoWidgetTextColor {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_color_dynamic

        @IgnoredOnParcel
        override val serializedName: String = "DYNAMIC"

        @IgnoredOnParcel
        override val serializedValue: String = type.name
    }

    @Parcelize
    data class Palette(
        val type: MatchPhotoColorType = MatchPhotoColorType.DOMINANT,
    ) : PhotoWidgetTextColor {

        @IgnoredOnParcel
        override val label: Int = R.string.photo_widget_configure_color_palette

        @IgnoredOnParcel
        override val serializedName: String = "PALETTE"

        @IgnoredOnParcel
        override val serializedValue: String = type.name
    }

    companion object {

        // White to match the launcher color for app names
        val DEFAULT: PhotoWidgetTextColor = Colored(colorHex = "FFFFFF")

        val entries: List<PhotoWidgetTextColor> by lazy {
            buildList {
                add(DEFAULT)

                if (DynamicColors.isDynamicColorAvailable()) {
                    add(Dynamic())
                }

                add(Palette())
            }
        }

        fun fromSerialized(serializedName: String?, serializedValue: String?): PhotoWidgetTextColor {
            return when (entries.firstOrNull { it.serializedName == serializedName }) {
                is Colored ->
                    serializedValue
                        ?.takeIf { value -> runCatching { "#$value".toColorInt() }.isSuccess }
                        ?.let(::Colored)
                        ?: DEFAULT

                is Dynamic -> Dynamic(
                    type = enumValueOfOrNull<DynamicColorType>(serializedValue)
                        ?: DynamicColorType.PRIMARY_INVERSE,
                )

                is Palette -> Palette(
                    type = enumValueOfOrNull<MatchPhotoColorType>(serializedValue)
                        ?: MatchPhotoColorType.DOMINANT,
                )

                null -> DEFAULT
            }
        }
    }
}

/**
 * The color used to draw the text. [colorPalette] is only invoked for
 * [PhotoWidgetTextColor.Palette], and must describe the photo the text is drawn over.
 */
@ColorInt
fun PhotoWidgetTextColor.resolve(context: Context, colorPalette: () -> ColorPalette): Int = when (this) {
    is PhotoWidgetTextColor.Colored -> "#$colorHex".toColorInt()
    is PhotoWidgetTextColor.Dynamic -> context.getDynamicAttributeColor(type.colorAttr)
    is PhotoWidgetTextColor.Palette -> colorPalette().colorForType(type)
}
