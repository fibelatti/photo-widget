package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
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
    borderColorHex: String? = null,
    borderWidth: Int = 0,
    badge: @Composable BoxScope.() -> Unit = {},
    isLoading: Boolean = false,
) {
    AsyncPhotoViewer(
        data = photo?.getPhotoPath(),
        dataKey = arrayOf(
            photo,
            shapeId,
            aspectRatio,
            cornerRadius,
            opacity,
            blackAndWhite,
            borderColorHex,
            borderWidth,
        ),
        isLoading = isLoading,
        contentScale = if (aspectRatio.isConstrained) {
            ContentScale.FillWidth
        } else {
            ContentScale.Fit
        },
        modifier = modifier.aspectRatio(ratio = aspectRatio.aspectRatio),
        transformer = { bitmap ->
            bitmap?.run {
                if (PhotoWidgetAspectRatio.SQUARE == aspectRatio) {
                    withPolygonalShape(
                        shapeId = shapeId,
                        opacity = opacity,
                        blackAndWhite = blackAndWhite,
                        borderColorHex = borderColorHex,
                        borderWidth = borderWidth,
                    )
                } else {
                    withRoundedCorners(
                        aspectRatio = aspectRatio,
                        radius = cornerRadius,
                        opacity = opacity,
                        blackAndWhite = blackAndWhite,
                        borderColorHex = borderColorHex,
                        borderWidth = borderWidth,
                    )
                }
            }
        },
        badge = badge,
    )
}
