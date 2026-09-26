package com.fibelatti.photowidget.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Parcelable
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.fibelatti.photowidget.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetText : Parcelable {

    val value: String
    val size: Int

    /** The Google Fonts family used to render the text, or `null` to use the system default. */
    val fontFamily: String?
    val color: PhotoWidgetTextColor
    val position: PhotoWidgetTextPosition
    val horizontalOffset: Int
    val verticalOffset: Int
    val hasShadow: Boolean
    val maxWidth: Int

    val serializedName: String

    @Parcelize
    data object None : PhotoWidgetText {

        @IgnoredOnParcel
        override val value: String = ""

        @IgnoredOnParcel
        override val size: Int = 0

        @IgnoredOnParcel
        override val fontFamily: String? = null

        @IgnoredOnParcel
        override val color: PhotoWidgetTextColor = PhotoWidgetTextColor.DEFAULT

        @IgnoredOnParcel
        override val position: PhotoWidgetTextPosition = PhotoWidgetTextPosition.BOTTOM

        @IgnoredOnParcel
        override val horizontalOffset: Int = 0

        @IgnoredOnParcel
        override val verticalOffset: Int = 0

        @IgnoredOnParcel
        override val hasShadow: Boolean = false

        @IgnoredOnParcel
        override val maxWidth: Int = 0

        @IgnoredOnParcel
        override val serializedName: String = "NONE"
    }

    @Parcelize
    data class Label(
        override val value: String = "",
        override val size: Int = 12,
        override val position: PhotoWidgetTextPosition = PhotoWidgetTextPosition.BOTTOM,
        override val verticalOffset: Int = 0,
        override val hasShadow: Boolean = true,
        override val fontFamily: String? = null,
        override val color: PhotoWidgetTextColor = PhotoWidgetTextColor.DEFAULT,
    ) : PhotoWidgetText {

        // Always centered
        @IgnoredOnParcel
        override val horizontalOffset: Int = 0

        // Fixed max width since measuring the widget is unreliable
        @IgnoredOnParcel
        override val maxWidth: Int = 200

        @IgnoredOnParcel
        override val serializedName: String = "LABEL"
    }

    companion object {

        val DEFAULT: PhotoWidgetText = None

        val SIZE_RANGE: IntRange = 10..40
        val VERTICAL_OFFSET_RANGE: IntRange = -40..40

        val entries: List<PhotoWidgetText> by lazy {
            listOf(None, Label())
        }

        fun fromSerializedName(serializedName: String?): PhotoWidgetText {
            return entries.firstOrNull { it.serializedName == serializedName } ?: DEFAULT
        }
    }
}

enum class PhotoWidgetTextPosition(@StringRes val label: Int) {
    TOP(label = R.string.photo_widget_configure_text_position_top),
    CENTER(label = R.string.photo_widget_configure_text_position_center),
    BOTTOM(label = R.string.photo_widget_configure_text_position_bottom),
    ;

    val verticalOffsetRange: IntRange
        get() = when (this) {
            TOP -> 0..PhotoWidgetText.VERTICAL_OFFSET_RANGE.last
            CENTER -> PhotoWidgetText.VERTICAL_OFFSET_RANGE
            BOTTOM -> PhotoWidgetText.VERTICAL_OFFSET_RANGE.first..0
        }
}

/**
 * The (top, bottom) padding, in dp, that moves the text [PhotoWidgetText.verticalOffset] dp from its
 * [PhotoWidgetText.position]: down when positive, up when negative.
 *
 * Padding applied to one side of a centered view only moves its content by half, so it is doubled
 * for [PhotoWidgetTextPosition.CENTER].
 */
val PhotoWidgetText.verticalPadding: Pair<Int, Int>
    get() {
        val padding: Int = when (position) {
            PhotoWidgetTextPosition.CENTER -> abs(verticalOffset) * 2
            else -> abs(verticalOffset)
        }

        return when {
            verticalOffset > 0 -> padding to 0
            verticalOffset < 0 -> 0 to padding
            else -> 0 to 0
        }
    }

/**
 * [color] is the resolved value of [PhotoWidgetText.color], see [PhotoWidgetTextColor.resolve].
 */
fun PhotoWidgetText.textToBitmap(context: Context, typeface: Typeface, @ColorInt color: Int): Bitmap {
    val staticLayout: StaticLayout = staticLayout(context = context, typeface = typeface, color = color)
    val output: Bitmap = createBitmap(staticLayout.width, staticLayout.height)

    Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
        withTranslation(x = staticLayout.width / 2f) {
            staticLayout.draw(this)
        }
    }

    return output
}

private fun PhotoWidgetText.staticLayout(
    context: Context,
    typeface: Typeface,
    @ColorInt color: Int,
): StaticLayout {
    val textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size.toFloat(),
            context.resources.displayMetrics,
        )
        textAlign = Paint.Align.CENTER

        this.typeface = typeface
        this.color = color

        if (hasShadow) {
            // Match the "Glow" look from the system launcher
            setShadowLayer(3f, 1f, 0f, "#B3000000".toColorInt())
        }
    }
    val width: Int = (maxWidth * context.resources.displayMetrics.density).roundToInt()

    return StaticLayout.Builder
        .obtain(
            /* source = */ value,
            /* start = */ 0,
            /* end = */ value.length,
            /* paint = */ textPaint,
            /* width = */ width,
        )
        .build()
}

/**
 * Bytes the bitmap produced by [textToBitmap] will occupy in a `RemoteViews` update, so the render
 * budget can account for the label alongside the photo. Zero when the widget has no label.
 *
 * [typeface] must be the same one given to [textToBitmap], since it determines the text bounds.
 */
fun PhotoWidgetText.bitmapByteCount(context: Context, typeface: Typeface): Long {
    if (this is PhotoWidgetText.None) return 0

    // The color doesn't affect the text bounds, so it can be hardcoded
    val staticLayout: StaticLayout = staticLayout(context = context, typeface = typeface, color = Color.WHITE)

    return staticLayout.width.toLong() * staticLayout.height * BYTES_PER_PIXEL
}

private const val BYTES_PER_PIXEL: Int = 4
