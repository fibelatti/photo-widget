package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Send: ImageVector
    get() {
        if (_Send != null) {
            return _Send!!
        }
        _Send = ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.301f, 13.695f)
                lineTo(20.102f, 3.897f)
                moveTo(10.58f, 14.135f)
                lineTo(12.802f, 18.58f)
                curveTo(13.339f, 19.654f, 13.608f, 20.192f, 13.946f, 20.336f)
                curveTo(14.239f, 20.461f, 14.575f, 20.438f, 14.849f, 20.275f)
                curveTo(15.165f, 20.087f, 15.359f, 19.518f, 15.747f, 18.382f)
                lineTo(19.946f, 6.084f)
                curveTo(20.285f, 5.094f, 20.454f, 4.599f, 20.338f, 4.271f)
                curveTo(20.237f, 3.986f, 20.013f, 3.762f, 19.728f, 3.662f)
                curveTo(19.4f, 3.546f, 18.905f, 3.715f, 17.915f, 4.053f)
                lineTo(5.618f, 8.252f)
                curveTo(4.481f, 8.64f, 3.913f, 8.834f, 3.725f, 9.15f)
                curveTo(3.562f, 9.424f, 3.539f, 9.76f, 3.664f, 10.054f)
                curveTo(3.808f, 10.392f, 4.345f, 10.66f, 5.419f, 11.198f)
                lineTo(9.864f, 13.42f)
                curveTo(10.041f, 13.509f, 10.13f, 13.553f, 10.206f, 13.612f)
                curveTo(10.274f, 13.664f, 10.335f, 13.725f, 10.388f, 13.793f)
                curveTo(10.447f, 13.87f, 10.491f, 13.958f, 10.58f, 14.135f)
                close()
            }
        }.build()

        return _Send!!
    }

@Suppress("ObjectPropertyName")
private var _Send: ImageVector? = null
