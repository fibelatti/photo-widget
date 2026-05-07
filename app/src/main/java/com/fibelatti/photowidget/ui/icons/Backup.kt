package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Backup: ImageVector
    get() {
        if (_Backup != null) {
            return _Backup!!
        }
        _Backup = ImageVector.Builder(
            name = "Backup",
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
                moveTo(17f, 10f)
                verticalLineTo(4.6f)
                curveTo(17f, 4.04f, 17f, 3.76f, 16.891f, 3.546f)
                curveTo(16.795f, 3.358f, 16.642f, 3.205f, 16.454f, 3.109f)
                curveTo(16.24f, 3f, 15.96f, 3f, 15.4f, 3f)
                horizontalLineTo(8.6f)
                curveTo(8.04f, 3f, 7.76f, 3f, 7.546f, 3.109f)
                curveTo(7.358f, 3.205f, 7.205f, 3.358f, 7.109f, 3.546f)
                curveTo(7f, 3.76f, 7f, 4.04f, 7f, 4.6f)
                verticalLineTo(10f)
                moveTo(10.5f, 7f)
                verticalLineTo(6f)
                moveTo(13.5f, 7f)
                verticalLineTo(6f)
                moveTo(11.4f, 21f)
                horizontalLineTo(12.6f)
                curveTo(14.84f, 21f, 15.96f, 21f, 16.816f, 20.564f)
                curveTo(17.569f, 20.181f, 18.181f, 19.569f, 18.564f, 18.816f)
                curveTo(19f, 17.96f, 19f, 16.84f, 19f, 14.6f)
                verticalLineTo(11.6f)
                curveTo(19f, 11.04f, 19f, 10.76f, 18.891f, 10.546f)
                curveTo(18.795f, 10.358f, 18.642f, 10.205f, 18.454f, 10.109f)
                curveTo(18.24f, 10f, 17.96f, 10f, 17.4f, 10f)
                horizontalLineTo(6.6f)
                curveTo(6.04f, 10f, 5.76f, 10f, 5.546f, 10.109f)
                curveTo(5.358f, 10.205f, 5.205f, 10.358f, 5.109f, 10.546f)
                curveTo(5f, 10.76f, 5f, 11.04f, 5f, 11.6f)
                verticalLineTo(14.6f)
                curveTo(5f, 16.84f, 5f, 17.96f, 5.436f, 18.816f)
                curveTo(5.819f, 19.569f, 6.431f, 20.181f, 7.184f, 20.564f)
                curveTo(8.04f, 21f, 9.16f, 21f, 11.4f, 21f)
                close()
            }
        }.build()

        return _Backup!!
    }

@Suppress("ObjectPropertyName")
private var _Backup: ImageVector? = null
