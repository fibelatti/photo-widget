package com.fibelatti.photowidget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.foundation.Shapes

@Composable
fun ShapeListItem(
    headlineText: String,
    currentValue: String,
    shapeRotation: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.StandaloneShape,
) {
    ListItem(
        headlineText = headlineText,
        trailingContent = {
            ColoredShape(
                shapeId = currentValue,
                shapeRotation = shapeRotation,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        },
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
    )
}
