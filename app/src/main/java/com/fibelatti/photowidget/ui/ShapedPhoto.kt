package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.withPolygonalShape
import com.fibelatti.photowidget.platform.withRoundedCorners

@Composable
fun ShapedPhoto(
    photo: LocalPhoto?,
    aspectRatio: PhotoWidgetAspectRatio,
    shapeId: String,
    cornerRadius: Float,
    opacity: Float,
    modifier: Modifier = Modifier,
    blackAndWhite: Boolean = false,
    border: PhotoWidgetBorder = PhotoWidgetBorder.None,
    badge: @Composable BoxScope.() -> Unit = {},
    isLoading: Boolean = false,
) {
    val localContext = LocalContext.current

    AsyncPhotoViewer(
        data = photo?.getPhotoPath(),
        dataKey = arrayOf(
            photo,
            shapeId,
            aspectRatio,
            cornerRadius,
            opacity,
            blackAndWhite,
            border,
        ),
        isLoading = isLoading,
        contentScale = if (aspectRatio.isConstrained) {
            ContentScale.FillWidth
        } else {
            ContentScale.Fit
        },
        modifier = modifier.aspectRatio(ratio = aspectRatio.aspectRatio),
        transformer = { bitmap ->
            val borderColor = when (border) {
                is PhotoWidgetBorder.None -> null
                is PhotoWidgetBorder.Color -> "#${border.colorHex}".toColorInt()
                is PhotoWidgetBorder.Dynamic -> localContext.getDynamicAttributeColor(
                    com.google.android.material.R.attr.colorPrimaryInverse,
                )
            }
            val borderPercent = border.getBorderPercent()

            bitmap?.run {
                if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                    withPolygonalShape(
                        shapeId = shapeId,
                        opacity = opacity,
                        blackAndWhite = blackAndWhite,
                        borderColor = borderColor,
                        borderPercent = borderPercent,
                    )
                } else {
                    withRoundedCorners(
                        aspectRatio = aspectRatio,
                        radius = cornerRadius,
                        opacity = opacity,
                        blackAndWhite = blackAndWhite,
                        borderColor = borderColor,
                        borderPercent = borderPercent,
                    )
                }
            }
        },
        badge = badge,
    )
}
