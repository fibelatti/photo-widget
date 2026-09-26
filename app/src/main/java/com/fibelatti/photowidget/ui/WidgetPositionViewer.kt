package com.fibelatti.photowidget.ui

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.PhotoWidgetTextColor
import com.fibelatti.photowidget.model.PhotoWidgetTextPosition
import com.fibelatti.photowidget.model.resolve
import com.fibelatti.photowidget.model.textToBitmap
import com.fibelatti.photowidget.model.verticalPadding
import com.fibelatti.photowidget.platform.ColorPalette
import com.fibelatti.photowidget.platform.GoogleFontsLoader
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.foundation.dpToPx
import kotlin.math.abs

@Composable
fun WidgetPositionViewer(
    photoWidget: PhotoWidget,
    modifier: Modifier = Modifier,
    areaColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val areaRadius: Float = 28.dp.dpToPx()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val cornerRadius = CornerRadius(areaRadius)
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
                onDrawWithContent {
                    drawRoundRect(
                        color = areaColor,
                        cornerRadius = cornerRadius,
                        style = stroke,
                    )
                    drawContent()
                }
            }
            .clip(shape = RoundedCornerShape(size = areaRadius)),
        contentAlignment = Alignment.Center,
    ) {
        val verticalOffset: Dp = abs(photoWidget.verticalOffset).dp * PhotoWidget.POSITIONING_MULTIPLIER
        val horizontalOffset: Dp = abs(photoWidget.horizontalOffset).dp * PhotoWidget.POSITIONING_MULTIPLIER

        Box(
            modifier = Modifier
                .padding(
                    start = if (photoWidget.horizontalOffset > 0) horizontalOffset else 0.dp,
                    top = if (photoWidget.verticalOffset > 0) verticalOffset else 0.dp,
                    end = if (photoWidget.horizontalOffset < 0) horizontalOffset else 0.dp,
                    bottom = if (photoWidget.verticalOffset < 0) verticalOffset else 0.dp,
                )
                .padding(all = photoWidget.padding.dp * PhotoWidget.POSITIONING_MULTIPLIER),
            contentAlignment = Alignment.Center,
        ) {
            if (photoWidget.currentPhoto != null) {
                ShapedPhoto(
                    photo = photoWidget.currentPhoto,
                    aspectRatio = photoWidget.aspectRatio,
                    shapeId = photoWidget.shapeId,
                    shapeRotation = photoWidget.shapeRotation,
                    cornerRadius = photoWidget.cornerRadius,
                    colors = photoWidget.colors,
                    border = photoWidget.border,
                )
            } else {
                Image(
                    bitmap = rememberSampleBitmap()
                        .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                        .asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        if (photoWidget.text is PhotoWidgetText.Label) {
            val labelTypeface: Typeface by rememberLabelTypeface(fontFamily = photoWidget.text.fontFamily)
            val labelColor: Int? = rememberLabelColor(color = photoWidget.text.color, photo = photoWidget.currentPhoto)

            if (labelColor != null) {
                val localContext = LocalContext.current
                val labelBitmap: ImageBitmap = remember(photoWidget.text, labelTypeface, labelColor) {
                    photoWidget.text
                        .textToBitmap(context = localContext, typeface = labelTypeface, color = labelColor)
                        .asImageBitmap()
                }
                val (topPadding: Int, bottomPadding: Int) = photoWidget.text.verticalPadding

                Image(
                    bitmap = labelBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .align(
                            when (photoWidget.text.position) {
                                PhotoWidgetTextPosition.TOP -> Alignment.TopCenter
                                PhotoWidgetTextPosition.CENTER -> Alignment.Center
                                PhotoWidgetTextPosition.BOTTOM -> Alignment.BottomCenter
                            },
                        )
                        .padding(
                            top = topPadding.dp,
                            bottom = bottomPadding.dp,
                        ),
                )
            }
        }
    }
}

/**
 * The resolved [color], or `null` while the photo it depends on is still decoding. It is matched
 * against [photo], or against the sample photo when there is none, the same photo that
 * [WidgetPositionViewer] displays.
 */
@ColorInt
@Composable
private fun rememberLabelColor(color: PhotoWidgetTextColor, photo: LocalPhoto?): Int? {
    val localContext = LocalContext.current

    val colorPalette: ColorPalette? = if (color is PhotoWidgetTextColor.Palette) {
        val photoBitmap: Bitmap? = if (photo != null) rememberPhotoBitmap(photo = photo) else rememberSampleBitmap()
        remember(photoBitmap) { photoBitmap?.let { bitmap -> getColorPalette(bitmap) } }
    } else {
        null
    }

    return remember(color, colorPalette) {
        if (color is PhotoWidgetTextColor.Palette && colorPalette == null) {
            null
        } else {
            color.resolve(context = localContext, colorPalette = { requireNotNull(colorPalette) })
        }
    }
}

/**
 * The typeface of [fontFamily], drawn with [Typeface.DEFAULT] until it finishes loading unless it
 * was already loaded.
 */
@Composable
private fun rememberLabelTypeface(fontFamily: String?): State<Typeface> {
    val localContext = LocalContext.current
    val localInspectionMode: Boolean = LocalInspectionMode.current
    val googleFontsLoader: GoogleFontsLoader by remember {
        lazy { entryPoint<PhotoWidgetEntryPoint>(localContext).googleFontsLoader() }
    }

    val initialValue: Typeface = if (localInspectionMode || fontFamily == null) {
        Typeface.DEFAULT
    } else {
        googleFontsLoader.peekTypeface(fontFamily = fontFamily) ?: Typeface.DEFAULT
    }

    return produceState(initialValue = initialValue, fontFamily) {
        if (localInspectionMode) return@produceState

        value = googleFontsLoader.getTypeface(fontFamily = fontFamily)
    }
}
