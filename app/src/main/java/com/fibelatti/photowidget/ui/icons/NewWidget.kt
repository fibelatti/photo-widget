package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.NewWidget: ImageVector
    get() {
        if (_NewWidget != null) {
            return _NewWidget!!
        }
        _NewWidget = ImageVector.Builder(
            name = "NewWidget",
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
                moveTo(17f, 14f)
                verticalLineTo(20f)
                moveTo(14f, 17f)
                horizontalLineTo(20f)
                moveTo(15.6f, 10f)
                horizontalLineTo(18.4f)
                curveTo(18.96f, 10f, 19.24f, 10f, 19.454f, 9.891f)
                curveTo(19.642f, 9.795f, 19.795f, 9.642f, 19.891f, 9.454f)
                curveTo(20f, 9.24f, 20f, 8.96f, 20f, 8.4f)
                verticalLineTo(5.6f)
                curveTo(20f, 5.04f, 20f, 4.76f, 19.891f, 4.546f)
                curveTo(19.795f, 4.358f, 19.642f, 4.205f, 19.454f, 4.109f)
                curveTo(19.24f, 4f, 18.96f, 4f, 18.4f, 4f)
                horizontalLineTo(15.6f)
                curveTo(15.04f, 4f, 14.76f, 4f, 14.546f, 4.109f)
                curveTo(14.358f, 4.205f, 14.205f, 4.358f, 14.109f, 4.546f)
                curveTo(14f, 4.76f, 14f, 5.04f, 14f, 5.6f)
                verticalLineTo(8.4f)
                curveTo(14f, 8.96f, 14f, 9.24f, 14.109f, 9.454f)
                curveTo(14.205f, 9.642f, 14.358f, 9.795f, 14.546f, 9.891f)
                curveTo(14.76f, 10f, 15.04f, 10f, 15.6f, 10f)
                close()
                moveTo(5.6f, 10f)
                horizontalLineTo(8.4f)
                curveTo(8.96f, 10f, 9.24f, 10f, 9.454f, 9.891f)
                curveTo(9.642f, 9.795f, 9.795f, 9.642f, 9.891f, 9.454f)
                curveTo(10f, 9.24f, 10f, 8.96f, 10f, 8.4f)
                verticalLineTo(5.6f)
                curveTo(10f, 5.04f, 10f, 4.76f, 9.891f, 4.546f)
                curveTo(9.795f, 4.358f, 9.642f, 4.205f, 9.454f, 4.109f)
                curveTo(9.24f, 4f, 8.96f, 4f, 8.4f, 4f)
                horizontalLineTo(5.6f)
                curveTo(5.04f, 4f, 4.76f, 4f, 4.546f, 4.109f)
                curveTo(4.358f, 4.205f, 4.205f, 4.358f, 4.109f, 4.546f)
                curveTo(4f, 4.76f, 4f, 5.04f, 4f, 5.6f)
                verticalLineTo(8.4f)
                curveTo(4f, 8.96f, 4f, 9.24f, 4.109f, 9.454f)
                curveTo(4.205f, 9.642f, 4.358f, 9.795f, 4.546f, 9.891f)
                curveTo(4.76f, 10f, 5.04f, 10f, 5.6f, 10f)
                close()
                moveTo(5.6f, 20f)
                horizontalLineTo(8.4f)
                curveTo(8.96f, 20f, 9.24f, 20f, 9.454f, 19.891f)
                curveTo(9.642f, 19.795f, 9.795f, 19.642f, 9.891f, 19.454f)
                curveTo(10f, 19.24f, 10f, 18.96f, 10f, 18.4f)
                verticalLineTo(15.6f)
                curveTo(10f, 15.04f, 10f, 14.76f, 9.891f, 14.546f)
                curveTo(9.795f, 14.358f, 9.642f, 14.205f, 9.454f, 14.109f)
                curveTo(9.24f, 14f, 8.96f, 14f, 8.4f, 14f)
                horizontalLineTo(5.6f)
                curveTo(5.04f, 14f, 4.76f, 14f, 4.546f, 14.109f)
                curveTo(4.358f, 14.205f, 4.205f, 14.358f, 4.109f, 14.546f)
                curveTo(4f, 14.76f, 4f, 15.04f, 4f, 15.6f)
                verticalLineTo(18.4f)
                curveTo(4f, 18.96f, 4f, 19.24f, 4.109f, 19.454f)
                curveTo(4.205f, 19.642f, 4.358f, 19.795f, 4.546f, 19.891f)
                curveTo(4.76f, 20f, 5.04f, 20f, 5.6f, 20f)
                close()
            }
        }.build()

        return _NewWidget!!
    }

@Suppress("ObjectPropertyName")
private var _NewWidget: ImageVector? = null
