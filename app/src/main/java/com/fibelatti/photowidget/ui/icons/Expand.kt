package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Expand: ImageVector
    get() {
        if (_Expand != null) {
            return _Expand!!
        }
        _Expand = ImageVector.Builder(
            name = "Expand",
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
                moveTo(14f, 10f)
                lineTo(21f, 3f)
                moveTo(21f, 3f)
                horizontalLineTo(16.5f)
                moveTo(21f, 3f)
                verticalLineTo(7.5f)
                moveTo(10f, 14f)
                lineTo(3f, 21f)
                moveTo(3f, 21f)
                horizontalLineTo(7.5f)
                moveTo(3f, 21f)
                lineTo(3f, 16.5f)
            }
        }.build()

        return _Expand!!
    }

@Suppress("ObjectPropertyName")
private var _Expand: ImageVector? = null
