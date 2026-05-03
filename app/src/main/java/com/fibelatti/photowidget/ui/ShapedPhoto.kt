package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.transform.Transformation
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.platform.PolygonalShapeTransformation
import com.fibelatti.photowidget.platform.RoundedCornersTransformation
import com.fibelatti.photowidget.platform.getDynamicAttributeColor

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

    val resolvedDynamicBorderColor: Int? = (border as? PhotoWidgetBorder.Dynamic)?.let {
        localContext.getDynamicAttributeColor(it.type.colorAttr)
    }

    val transformations: List<Transformation> = remember(
        aspectRatio,
        shapeId,
        cornerRadius,
        colors,
        border,
        resolvedDynamicBorderColor,
        localDensity,
    ) {
        val transformation: Transformation = if (aspectRatio == PhotoWidgetAspectRatio.SQUARE) {
            PolygonalShapeTransformation(
                context = localContext,
                shapeId = shapeId,
                colors = colors,
                border = border,
                resolvedDynamicBorderColor = resolvedDynamicBorderColor,
            )
        } else {
            RoundedCornersTransformation(
                aspectRatio = aspectRatio,
                radius = cornerRadius * localDensity,
                colors = colors,
                border = border,
                resolvedDynamicBorderColor = resolvedDynamicBorderColor,
            )
        }
        listOf(transformation)
    }

    AsyncPhotoViewer(
        data = photo?.getPhotoPath(),
        isLoading = isLoading,
        contentScale = if (aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        },
        modifier = modifier.fillMaxSize(),
        constraintMode = AsyncPhotoViewer.BitmapSizeConstraintMode.DISPLAY,
        transformations = transformations,
    )
}
