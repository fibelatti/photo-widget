package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
        modifier = modifier.drawWithContent {
            drawPath(
                path = PhotoWidgetShapeBuilder.getShapePath(
                    shapeId = shapeId,
                    width = size.width,
                    height = size.height,
                ).asComposePath(),
                color = color,
            )

            drawContent()
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
