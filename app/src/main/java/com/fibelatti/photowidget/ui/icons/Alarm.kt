package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Alarm: ImageVector
    get() {
        if (_Alarm != null) {
            return _Alarm!!
        }
        _Alarm = ImageVector.Builder(
            name = "Alarm",
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
                moveTo(3f, 5.5f)
                lineTo(5f, 3.5f)
                moveTo(21f, 5.5f)
                lineTo(19f, 3.5f)
                moveTo(12f, 8.5f)
                verticalLineTo(12.5f)
                lineTo(14f, 14.5f)
                moveTo(20f, 12.5f)
                curveTo(20f, 16.918f, 16.418f, 20.5f, 12f, 20.5f)
                curveTo(7.582f, 20.5f, 4f, 16.918f, 4f, 12.5f)
                curveTo(4f, 8.082f, 7.582f, 4.5f, 12f, 4.5f)
                curveTo(16.418f, 4.5f, 20f, 8.082f, 20f, 12.5f)
                close()
            }
        }.build()

        return _Alarm!!
    }

@Suppress("ObjectPropertyName")
private var _Alarm: ImageVector? = null
