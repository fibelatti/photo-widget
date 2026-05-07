package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Default: ImageVector
    get() {
        if (_Default != null) {
            return _Default!!
        }
        _Default = ImageVector.Builder(
            name = "Default",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 6.2f)
                curveTo(5f, 5.08f, 5f, 4.52f, 5.218f, 4.092f)
                curveTo(5.41f, 3.716f, 5.716f, 3.41f, 6.092f, 3.218f)
                curveTo(6.52f, 3f, 7.08f, 3f, 8.2f, 3f)
                horizontalLineTo(15.8f)
                curveTo(16.92f, 3f, 17.48f, 3f, 17.908f, 3.218f)
                curveTo(18.284f, 3.41f, 18.59f, 3.716f, 18.782f, 4.092f)
                curveTo(19f, 4.52f, 19f, 5.08f, 19f, 6.2f)
                verticalLineTo(21f)
                lineTo(12f, 16f)
                lineTo(5f, 21f)
                verticalLineTo(6.2f)
                close()
            }
        }.build()

        return _Default!!
    }

@Suppress("ObjectPropertyName")
private var _Default: ImageVector? = null
