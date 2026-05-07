package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Export: ImageVector
    get() {
        if (_Export != null) {
            return _Export!!
        }
        _Export = ImageVector.Builder(
            name = "Export",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 10f)
                horizontalLineTo(6.2f)
                curveTo(5.08f, 10f, 4.52f, 10f, 4.092f, 10.218f)
                curveTo(3.716f, 10.41f, 3.41f, 10.716f, 3.218f, 11.092f)
                curveTo(3f, 11.52f, 3f, 12.08f, 3f, 13.2f)
                verticalLineTo(16.8f)
                curveTo(3f, 17.92f, 3f, 18.48f, 3.218f, 18.908f)
                curveTo(3.41f, 19.284f, 3.716f, 19.59f, 4.092f, 19.782f)
                curveTo(4.52f, 20f, 5.08f, 20f, 6.2f, 20f)
                horizontalLineTo(17.8f)
                curveTo(18.92f, 20f, 19.48f, 20f, 19.908f, 19.782f)
                curveTo(20.284f, 19.59f, 20.59f, 19.284f, 20.782f, 18.908f)
                curveTo(21f, 18.48f, 21f, 17.92f, 21f, 16.8f)
                verticalLineTo(13.2f)
                curveTo(21f, 12.08f, 21f, 11.52f, 20.782f, 11.092f)
                curveTo(20.59f, 10.716f, 20.284f, 10.41f, 19.908f, 10.218f)
                curveTo(19.48f, 10f, 18.92f, 10f, 17.8f, 10f)
                horizontalLineTo(17f)
                moveTo(12f, 4f)
                verticalLineTo(16f)
                moveTo(12f, 4f)
                lineTo(9f, 7f)
                moveTo(12f, 4f)
                lineTo(15f, 7f)
            }
        }.build()

        return _Export!!
    }

@Suppress("ObjectPropertyName")
private var _Export: ImageVector? = null
