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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import kotlin.math.roundToInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetText : Parcelable {

    val value: String
    val size: Int

    /** The Google Fonts family used to render the text, or `null` to use the system default. */
    val fontFamily: String?
    val colorHex: String
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
        override val colorHex: String = "00000000"

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
        override val verticalOffset: Int = 0,
        override val hasShadow: Boolean = true,
        override val fontFamily: String? = null,
    ) : PhotoWidgetText {

        // Always white to match the launcher color
        @IgnoredOnParcel
        override val colorHex: String = "FFFFFF"

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

        val entries: List<PhotoWidgetText> by lazy {
            listOf(None, Label())
        }

        fun fromSerializedName(serializedName: String?): PhotoWidgetText {
            return entries.firstOrNull { it.serializedName == serializedName } ?: DEFAULT
        }
    }
}

fun PhotoWidgetText.textToBitmap(context: Context, typeface: Typeface): Bitmap {
    val staticLayout: StaticLayout = staticLayout(context = context, typeface = typeface)
    val output: Bitmap = createBitmap(staticLayout.width, staticLayout.height)

    Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
        withTranslation(x = staticLayout.width / 2f) {
            staticLayout.draw(this)
        }
    }

    return output
}

private fun PhotoWidgetText.staticLayout(context: Context, typeface: Typeface): StaticLayout {
    val textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size.toFloat(),
            context.resources.displayMetrics,
        )
        textAlign = Paint.Align.CENTER

        this.typeface = typeface

        color = "#$colorHex".toColorInt()

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

    val staticLayout: StaticLayout = staticLayout(context = context, typeface = typeface)

    return staticLayout.width.toLong() * staticLayout.height * BYTES_PER_PIXEL
}

private const val BYTES_PER_PIXEL: Int = 4
