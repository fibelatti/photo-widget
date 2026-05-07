package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.HardDrive: ImageVector
    get() {
        if (_HardDrive != null) {
            return _HardDrive!!
        }
        _HardDrive = ImageVector.Builder(
            name = "HardDrive",
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
                moveTo(20.5f, 11f)
                lineTo(18.74f, 7.605f)
                curveTo(18.253f, 6.659f, 18.01f, 6.185f, 17.651f, 5.84f)
                curveTo(17.335f, 5.535f, 16.954f, 5.303f, 16.538f, 5.161f)
                curveTo(16.067f, 5f, 15.535f, 5f, 14.471f, 5f)
                horizontalLineTo(9.529f)
                curveTo(8.465f, 5f, 7.933f, 5f, 7.462f, 5.161f)
                curveTo(7.046f, 5.303f, 6.665f, 5.535f, 6.349f, 5.84f)
                curveTo(5.99f, 6.185f, 5.747f, 6.659f, 5.26f, 7.605f)
                lineTo(3.5f, 11f)
                moveTo(20.5f, 11f)
                curveTo(20.698f, 11.385f, 20.766f, 11.545f, 20.836f, 11.747f)
                curveTo(20.898f, 11.927f, 20.943f, 12.112f, 20.97f, 12.3f)
                curveTo(21f, 12.512f, 21f, 12.729f, 21f, 13.162f)
                verticalLineTo(14.2f)
                curveTo(21f, 15.88f, 21f, 16.72f, 20.673f, 17.362f)
                curveTo(20.385f, 17.927f, 19.927f, 18.385f, 19.362f, 18.673f)
                curveTo(18.72f, 19f, 17.88f, 19f, 16.2f, 19f)
                horizontalLineTo(7.8f)
                curveTo(6.12f, 19f, 5.28f, 19f, 4.638f, 18.673f)
                curveTo(4.074f, 18.385f, 3.615f, 17.927f, 3.327f, 17.362f)
                curveTo(3f, 16.72f, 3f, 15.88f, 3f, 14.2f)
                verticalLineTo(13.162f)
                curveTo(3f, 12.729f, 3f, 12.512f, 3.03f, 12.3f)
                curveTo(3.057f, 12.112f, 3.102f, 11.927f, 3.164f, 11.747f)
                curveTo(3.234f, 11.545f, 3.302f, 11.385f, 3.5f, 11f)
                moveTo(20.5f, 11f)
                horizontalLineTo(3.5f)
                moveTo(15f, 15f)
                horizontalLineTo(17f)
            }
        }.build()

        return _HardDrive!!
    }

@Suppress("ObjectPropertyName")
private var _HardDrive: ImageVector? = null
