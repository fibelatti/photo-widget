package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder

@Composable
fun ColoredShape(
    shapeId: String,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawWithCache {
                val shapePath = PhotoWidgetShapeBuilder.getShapePath(
                    shapeId = shapeId,
                    size = size.minDimension,
                ).asComposePath()

                onDrawWithContent {
                    drawPath(path = shapePath, color = color)
                    drawContent()
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
