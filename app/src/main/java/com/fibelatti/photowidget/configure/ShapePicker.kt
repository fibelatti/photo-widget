package com.fibelatti.photowidget.configure

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.DefaultSheetContent

@Composable
fun ShapePicker(
    onClick: (shapeId: String) -> Unit,
    modifier: Modifier = Modifier,
    selectedShapeId: String? = null,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.widget_defaults_shape),
        modifier = modifier,
    ) {
        val state = rememberLazyGridState()
        val shapeSize = 80.dp
        val spacing = 8.dp
        val rowCount = 4

        LazyHorizontalGrid(
            rows = GridCells.Fixed(count = rowCount),
            modifier = Modifier
                .height(height = (shapeSize * rowCount) + (spacing * (rowCount - 1)))
                .fillMaxWidth(),
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(PhotoWidgetShapeBuilder.shapes) { shape ->
                val color by animateColorAsState(
                    targetValue = if (shape.id == selectedShapeId || selectedShapeId == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "ShapePicker_SelectedColor",
                )
                ColoredShape(
                    shapeId = shape.id,
                    color = color,
                    modifier = Modifier
                        .size(shapeSize)
                        .clickable { onClick(shape.id) },
                )
            }
        }

        LaunchedEffect(Unit) {
            val selectedIndex = PhotoWidgetShapeBuilder.shapes.indexOfFirst { it.id == selectedShapeId }
            val visibleCount = state.layoutInfo.visibleItemsInfo.size

            if (selectedIndex > visibleCount) {
                state.scrollToItem(index = selectedIndex)
            }
        }
    }
}
