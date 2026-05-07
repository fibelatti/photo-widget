package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Translation: ImageVector
    get() {
        if (_Translation != null) {
            return _Translation!!
        }
        _Translation = ImageVector.Builder(
            name = "Translation",
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
                moveTo(20f, 15f)
                horizontalLineTo(19f)
                moveTo(14f, 15f)
                horizontalLineTo(19f)
                moveTo(17f, 13.5f)
                verticalLineTo(15f)
                moveTo(4.857f, 8f)
                horizontalLineTo(9.143f)
                moveTo(4f, 11f)
                lineTo(5.538f, 5.615f)
                curveTo(5.975f, 4.088f, 6.193f, 3.324f, 6.53f, 3.132f)
                curveTo(6.822f, 2.967f, 7.178f, 2.967f, 7.47f, 3.132f)
                curveTo(7.807f, 3.324f, 8.025f, 4.088f, 8.462f, 5.615f)
                lineTo(10f, 11f)
                moveTo(14f, 20.978f)
                curveTo(16.803f, 20.725f, 19f, 18.369f, 19f, 15.5f)
                verticalLineTo(15f)
                moveTo(20f, 20.978f)
                curveTo(18.076f, 20.804f, 16.438f, 19.64f, 15.6f, 18f)
                moveTo(14f, 7f)
                curveTo(14.932f, 7f, 15.398f, 7f, 15.765f, 7.152f)
                curveTo(16.255f, 7.355f, 16.645f, 7.745f, 16.848f, 8.235f)
                curveTo(17f, 8.602f, 17f, 9.068f, 17f, 10f)
                moveTo(7f, 15f)
                curveTo(7f, 15.932f, 7f, 16.398f, 7.152f, 16.765f)
                curveTo(7.355f, 17.255f, 7.745f, 17.645f, 8.235f, 17.848f)
                curveTo(8.602f, 18f, 9.068f, 18f, 10f, 18f)
            }
        }.build()

        return _Translation!!
    }

@Suppress("ObjectPropertyName")
private var _Translation: ImageVector? = null
