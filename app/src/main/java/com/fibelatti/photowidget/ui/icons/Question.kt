package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Question: ImageVector
    get() {
        if (_Question != null) {
            return _Question!!
        }
        _Question = ImageVector.Builder(
            name = "Question",
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
                moveTo(11.967f, 12.75f)
                curveTo(12.967f, 11.75f, 13.967f, 11.355f, 13.967f, 10.25f)
                curveTo(13.967f, 9.145f, 13.072f, 8.25f, 11.967f, 8.25f)
                curveTo(11.035f, 8.25f, 10.252f, 8.887f, 10.03f, 9.75f)
                moveTo(11.967f, 15.75f)
                horizontalLineTo(11.977f)
                moveTo(21.004f, 12f)
                curveTo(21.004f, 16.971f, 16.975f, 21f, 12.004f, 21f)
                curveTo(9.967f, 21f, 3.005f, 21f, 3.005f, 21f)
                curveTo(3.005f, 21f, 4.564f, 17.256f, 3.94f, 16.001f)
                curveTo(3.341f, 14.796f, 3.004f, 13.437f, 3.004f, 12f)
                curveTo(3.004f, 7.029f, 7.033f, 3f, 12.004f, 3f)
                curveTo(16.975f, 3f, 21.004f, 7.029f, 21.004f, 12f)
                close()
            }
        }.build()

        return _Question!!
    }

@Suppress("ObjectPropertyName")
private var _Question: ImageVector? = null
