package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.borderPercent
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.platform.colorForType
import com.fibelatti.photowidget.platform.getColorPalette
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners

@Composable
fun ShapedPhoto(
    photo: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Int,
    modifier: Modifier = Modifier,
    colors: PhotoWidgetColors = PhotoWidgetColors(),
    border: PhotoWidgetBorder = PhotoWidgetBorder.None,
    isLoading: Boolean = false,
) {
    val localContext = LocalContext.current
    val localDensity = LocalDensity.current.density

    AsyncPhotoViewer(
        data = photo?.getPhotoPath(),
        dataKey = arrayOf(
            photo?.photoId,
            photo?.getPhotoPath(),
            photo?.timestamp,
            aspectRatio,
            shapeId,
            cornerRadius,
            colors,
            border,
        ),
        isLoading = isLoading,
        contentScale = if (aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        },
        modifier = modifier.fillMaxSize(),
        constraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.DISPLAY,
        transformer = { bitmap ->
            val borderColor = when (border) {
                is PhotoWidgetBorder.None -> null
                is PhotoWidgetBorder.Color -> "#${border.colorHex}".toColorInt()
                is PhotoWidgetBorder.Dynamic -> localContext.getDynamicAttributeColor(border.type.colorAttr)
                is PhotoWidgetBorder.MatchPhoto -> getColorPalette(bitmap).colorForType(border.type)
            }
            val borderPercent = border.borderPercent()

            if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                bitmap.withPolygonalShape(
                    context = localContext,
                    shapeId = shapeId,
                    colors = colors,
                    borderColor = borderColor,
                    borderPercent = borderPercent,
                )
            } else {
                bitmap.withRoundedCorners(
                    aspectRatio = aspectRatio,
                    radius = cornerRadius * localDensity,
                    colors = colors,
                    borderColor = borderColor,
                    borderPercent = borderPercent,
                )
            }
        },
    )
}
