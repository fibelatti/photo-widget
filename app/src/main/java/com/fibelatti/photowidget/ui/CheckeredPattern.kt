package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * The classic checkered pattern used by image editors to represent transparent areas.
 */
@Composable
fun CheckeredPattern(
    modifier: Modifier = Modifier,
    cellSize: Dp = 8.dp,
    lightColor: Color = Color(0xFFFFFFFF),
    darkColor: Color = Color(0xFFCCCCCC),
) {
    Spacer(
        modifier = modifier
            .drawWithCache {
                val cellSizePx = cellSize.toPx()
                val columns = ceil(size.width / cellSizePx).toInt()
                val rows = ceil(size.height / cellSizePx).toInt()

                onDrawBehind {
                    drawRect(color = lightColor, size = size)
                    for (row in 0 until rows) {
                        for (column in 0 until columns) {
                            if ((row + column) % 2 == 1) {
                                drawRect(
                                    color = darkColor,
                                    topLeft = Offset(x = column * cellSizePx, y = row * cellSizePx),
                                    size = Size(width = cellSizePx, height = cellSizePx),
                                )
                            }
                        }
                    }
                }
            },
    )
}
