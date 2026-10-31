package com.fibelatti.photowidget.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetText : Parcelable {

    val size: Int

    val font: PhotoWidgetTextFont
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
        override val size: Int = 0

        @IgnoredOnParcel
        override val font: PhotoWidgetTextFont = PhotoWidgetTextFont.Default

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
        val value: String = "",
        override val size: Int = 12,
        override val position: PhotoWidgetTextPosition = PhotoWidgetTextPosition.BOTTOM,
        override val verticalOffset: Int = 0,
        override val hasShadow: Boolean = true,
        override val font: PhotoWidgetTextFont = PhotoWidgetTextFont.Default,
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

    /**
     * Displays the name of the current photo file, without its extension. Only available to
     * [PhotoWidgetSource.DIRECTORY] widgets, since other sources don't keep the original file name.
     */
    @Parcelize
    data class FileName(
        override val size: Int = 12,
        override val position: PhotoWidgetTextPosition = PhotoWidgetTextPosition.BOTTOM,
        override val verticalOffset: Int = 0,
        override val hasShadow: Boolean = true,
        override val font: PhotoWidgetTextFont = PhotoWidgetTextFont.Default,
        override val color: PhotoWidgetTextColor = PhotoWidgetTextColor.DEFAULT,
    ) : PhotoWidgetText {

        // Always centered
        @IgnoredOnParcel
        override val horizontalOffset: Int = 0

        // Fixed max width since measuring the widget is unreliable
        @IgnoredOnParcel
        override val maxWidth: Int = 200

        @IgnoredOnParcel
        override val serializedName: String = "FILE_NAME"
    }

    companion object {

        val DEFAULT: PhotoWidgetText = None

        val SIZE_RANGE: IntRange = 10..40
        val VERTICAL_OFFSET_RANGE: IntRange = -40..40

        val entries: List<PhotoWidgetText> by lazy {
            listOf(None, Label(), FileName())
        }

        fun entries(source: PhotoWidgetSource): List<PhotoWidgetText> {
            return entries.filter { it.isAvailable(source = source) }
        }

        fun fromSerializedName(serializedName: String?): PhotoWidgetText {
            return entries.firstOrNull { it.serializedName == serializedName } ?: DEFAULT
        }
    }
}

fun PhotoWidgetText.isAvailable(source: PhotoWidgetSource): Boolean {
    return this !is PhotoWidgetText.FileName || source == PhotoWidgetSource.DIRECTORY
}

/**
 * Returns this text, or [PhotoWidgetText.None] when it isn't available to [source].
 */
fun PhotoWidgetText.coerceText(source: PhotoWidgetSource): PhotoWidgetText {
    return if (isAvailable(source = source)) this else PhotoWidgetText.None
}

/**
 * Returns a copy of this text with the given style, or itself when it doesn't display any text.
 */
fun PhotoWidgetText.copyStyle(
    size: Int = this.size,
    position: PhotoWidgetTextPosition = this.position,
    verticalOffset: Int = this.verticalOffset,
    hasShadow: Boolean = this.hasShadow,
    font: PhotoWidgetTextFont = this.font,
    color: PhotoWidgetTextColor = this.color,
): PhotoWidgetText {
    return when (this) {
        is PhotoWidgetText.None -> this

        is PhotoWidgetText.Label -> copy(
            size = size,
            position = position,
            verticalOffset = verticalOffset,
            hasShadow = hasShadow,
            font = font,
            color = color,
        )

        is PhotoWidgetText.FileName -> copy(
            size = size,
            position = position,
            verticalOffset = verticalOffset,
            hasShadow = hasShadow,
            font = font,
            color = color,
        )
    }
}

/**
 * The text to display alongside [photo], or null when there's nothing to display.
 */
fun PhotoWidgetText.displayValue(photo: LocalPhoto?): String? {
    return when (this) {
        is PhotoWidgetText.None -> null

        is PhotoWidgetText.Label -> value

        is PhotoWidgetText.FileName -> photo?.displayName?.let { name: String ->
            // Keeps names that only have an extension, e.g. hidden files
            name.substringBeforeLast(delimiter = ".").ifEmpty { name }
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
 * Creates a [Bitmap] representation of this [PhotoWidgetText].
 *
 * @param context [Context] to create a [StaticLayout].
 * @param value the text to draw, see [displayValue].
 * @param typeface the [Typeface] to use.
 * @param color the resolved value of [PhotoWidgetText.color], see [PhotoWidgetTextColor.resolve].
 */
fun PhotoWidgetText.textToBitmap(
    context: Context,
    value: String,
    typeface: Typeface,
    @ColorInt color: Int,
): Bitmap {
    val staticLayout: StaticLayout = staticLayout(
        context = context,
        value = value,
        typeface = typeface,
        color = color,
    )
    val output: Bitmap = createBitmap(staticLayout.width, staticLayout.height)

    Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
        staticLayout.draw(this)
    }

    return output
}

private fun PhotoWidgetText.staticLayout(
    context: Context,
    value: String,
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
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .build()
}

/**
 * Bytes the bitmap produced by [textToBitmap] will occupy in a `RemoteViews` update, so the render
 * budget can account for the label alongside the photo. Zero when there's no text to display.
 *
 * [value] and [typeface] must be the same ones given to [textToBitmap], since they determine the
 * text bounds.
 */
fun PhotoWidgetText.bitmapByteCount(context: Context, value: String?, typeface: Typeface): Long {
    if (value == null) return 0

    // The color doesn't affect the text bounds, so it can be hardcoded
    val staticLayout: StaticLayout = staticLayout(
        context = context,
        value = value,
        typeface = typeface,
        color = Color.WHITE,
    )

    return staticLayout.width.toLong() * staticLayout.height * BYTES_PER_PIXEL
}

private const val BYTES_PER_PIXEL: Int = 4
