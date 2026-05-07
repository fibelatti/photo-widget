package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Trash: ImageVector
    get() {
        if (_Trash != null) {
            return _Trash!!
        }
        _Trash = ImageVector.Builder(
            name = "Trash",
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
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                moveTo(16f, 6f)
                lineTo(15.729f, 5.188f)
                curveTo(15.467f, 4.401f, 15.336f, 4.008f, 15.093f, 3.717f)
                curveTo(14.878f, 3.46f, 14.602f, 3.261f, 14.29f, 3.139f)
                curveTo(13.938f, 3f, 13.523f, 3f, 12.694f, 3f)
                horizontalLineTo(11.306f)
                curveTo(10.477f, 3f, 10.062f, 3f, 9.71f, 3.139f)
                curveTo(9.398f, 3.261f, 9.122f, 3.46f, 8.907f, 3.717f)
                curveTo(8.664f, 4.008f, 8.533f, 4.401f, 8.271f, 5.188f)
                lineTo(8f, 6f)
                moveTo(18f, 6f)
                verticalLineTo(16.2f)
                curveTo(18f, 17.88f, 18f, 18.72f, 17.673f, 19.362f)
                curveTo(17.385f, 19.927f, 16.927f, 20.385f, 16.362f, 20.673f)
                curveTo(15.72f, 21f, 14.88f, 21f, 13.2f, 21f)
                horizontalLineTo(10.8f)
                curveTo(9.12f, 21f, 8.28f, 21f, 7.638f, 20.673f)
                curveTo(7.074f, 20.385f, 6.615f, 19.927f, 6.327f, 19.362f)
                curveTo(6f, 18.72f, 6f, 17.88f, 6f, 16.2f)
                verticalLineTo(6f)
                moveTo(14f, 10f)
                verticalLineTo(17f)
                moveTo(10f, 10f)
                verticalLineTo(17f)
            }
        }.build()

        return _Trash!!
    }

@Suppress("ObjectPropertyName")
private var _Trash: ImageVector? = null
