package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Import: ImageVector
    get() {
        if (_Import != null) {
            return _Import!!
        }
        _Import = ImageVector.Builder(
            name = "Import",
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
                moveTo(12f, 16f)
                verticalLineTo(4f)
                moveTo(12f, 16f)
                lineTo(9f, 13f)
                moveTo(12f, 16f)
                lineTo(15f, 13f)
                moveTo(7f, 9f)
                horizontalLineTo(6.2f)
                curveTo(5.08f, 9f, 4.52f, 9f, 4.092f, 9.218f)
                curveTo(3.716f, 9.41f, 3.41f, 9.716f, 3.218f, 10.092f)
                curveTo(3f, 10.52f, 3f, 11.08f, 3f, 12.2f)
                verticalLineTo(16.8f)
                curveTo(3f, 17.92f, 3f, 18.48f, 3.218f, 18.908f)
                curveTo(3.41f, 19.284f, 3.716f, 19.59f, 4.092f, 19.782f)
                curveTo(4.52f, 20f, 5.08f, 20f, 6.2f, 20f)
                horizontalLineTo(17.8f)
                curveTo(18.92f, 20f, 19.48f, 20f, 19.908f, 19.782f)
                curveTo(20.284f, 19.59f, 20.59f, 19.284f, 20.782f, 18.908f)
                curveTo(21f, 18.48f, 21f, 17.92f, 21f, 16.8f)
                verticalLineTo(12.2f)
                curveTo(21f, 11.08f, 21f, 10.52f, 20.782f, 10.092f)
                curveTo(20.59f, 9.716f, 20.284f, 9.41f, 19.908f, 9.218f)
                curveTo(19.48f, 9f, 18.92f, 9f, 17.8f, 9f)
                horizontalLineTo(17f)
            }
        }.build()

        return _Import!!
    }

@Suppress("ObjectPropertyName")
private var _Import: ImageVector? = null
