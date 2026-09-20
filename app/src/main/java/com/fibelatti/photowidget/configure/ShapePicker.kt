package com.fibelatti.photowidget.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetShapeRotation
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.DefaultSheetContent
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.ui.preview.PreviewThemesAndColors
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun ShapePicker(
    onClick: (shapeId: String) -> Unit,
    modifier: Modifier = Modifier,
    selectedShapeId: String? = null,
    shapeRotation: Int = PhotoWidgetShapeRotation.DEFAULT,
) {
    DefaultSheetContent(
        title = stringResource(id = R.string.widget_defaults_shape),
        modifier = modifier,
    ) {
        val state = rememberLazyGridState()
        val shapeSize = 80.dp
        val spacing = 16.dp
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
            items(PhotoWidgetShapeBuilder.shapes, key = { shape -> shape.id }) { shape ->
                val shapeColor: Color = if (shape.id == selectedShapeId || selectedShapeId == null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                Box {
                    ColoredShape(
                        shapeId = shape.id,
                        shapeRotation = shapeRotation,
                        color = shapeColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(shapeSize)
                            .clickable { onClick(shape.id) },
                    )

                    if (shape.canRotate) {
                        Icon(
                            painter = painterResource(com.canhub.cropper.R.drawable.ic_rotate_right_24),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .8f),
                                    shape = CircleShape,
                                ),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        InformationalPanel(
            text = stringResource(R.string.widget_defaults_shape_rotation_hint),
            icon = painterResource(com.canhub.cropper.R.drawable.ic_rotate_right_24),
        )

        LaunchedEffect(Unit) {
            val selectedIndex = PhotoWidgetShapeBuilder.shapes.indexOfFirst { it.id == selectedShapeId }
            val visibleCount = state.layoutInfo.visibleItemsInfo.size

            if (selectedIndex > visibleCount) {
                state.scrollToItem(index = selectedIndex)
            }
        }
    }
}

@PreviewThemesAndColors
@Composable
private fun ShapePickerPreview() {
    ExtendedTheme {
        ShapePicker(
            onClick = {},
        )
    }
}
