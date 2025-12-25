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
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import kotlin.math.roundToInt
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface PhotoWidgetText : Parcelable {

    val value: String
    val size: Int
    val typeface: Int?
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
        override val typeface: Int? = null

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
    ) : PhotoWidgetText {

        // Always fallback to `Typeface.DEFAULT` to match the system
        @IgnoredOnParcel
        override val typeface: Int? = null

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

fun PhotoWidgetText.textToBitmap(context: Context): Bitmap {
    val textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            size.toFloat(),
            context.resources.displayMetrics,
        )
        textAlign = Paint.Align.CENTER

        typeface = this@textToBitmap.typeface?.let { ResourcesCompat.getFont(context, it) }
            ?: Typeface.DEFAULT

        color = "#$colorHex".toColorInt()

        if (hasShadow) {
            // Match the "Glow" look from the system launcher
            setShadowLayer(3f, 1f, 0f, "#B3000000".toColorInt())
        }
    }
    val width: Int = (maxWidth * context.resources.displayMetrics.density).roundToInt()

    val staticLayout: StaticLayout = StaticLayout.Builder
        .obtain(
            /* source = */ value,
            /* start = */ 0,
            /* end = */ value.length,
            /* paint = */ textPaint,
            /* width = */ width,
        )
        .build()

    val output: Bitmap = createBitmap(staticLayout.width, staticLayout.height)

    Canvas(output).apply {
        drawColor(Color.TRANSPARENT)
        withTranslation(x = width / 2f) {
            staticLayout.draw(this)
        }
    }

    return output
}
