package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.PrivacyPolicy: ImageVector
    get() {
        if (_PrivacyPolicy != null) {
            return _PrivacyPolicy!!
        }
        _PrivacyPolicy = ImageVector.Builder(
            name = "PrivacyPolicy",
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
                moveTo(13f, 3f)
                horizontalLineTo(8.2f)
                curveTo(7.08f, 3f, 6.52f, 3f, 6.092f, 3.218f)
                curveTo(5.716f, 3.41f, 5.41f, 3.716f, 5.218f, 4.092f)
                curveTo(5f, 4.52f, 5f, 5.08f, 5f, 6.2f)
                verticalLineTo(17.8f)
                curveTo(5f, 18.92f, 5f, 19.48f, 5.218f, 19.908f)
                curveTo(5.41f, 20.284f, 5.716f, 20.59f, 6.092f, 20.782f)
                curveTo(6.52f, 21f, 7.08f, 21f, 8.2f, 21f)
                horizontalLineTo(12f)
                moveTo(13f, 3f)
                lineTo(19f, 9f)
                moveTo(13f, 3f)
                verticalLineTo(7.4f)
                curveTo(13f, 7.96f, 13f, 8.24f, 13.109f, 8.454f)
                curveTo(13.205f, 8.642f, 13.358f, 8.795f, 13.546f, 8.891f)
                curveTo(13.76f, 9f, 14.04f, 9f, 14.6f, 9f)
                horizontalLineTo(19f)
                moveTo(19f, 9f)
                verticalLineTo(10f)
                moveTo(9f, 17f)
                horizontalLineTo(11f)
                moveTo(9f, 13f)
                horizontalLineTo(12f)
                moveTo(9f, 9f)
                horizontalLineTo(10f)
                moveTo(21f, 15.21f)
                curveTo(20.932f, 15.214f, 20.736f, 15.21f, 20.667f, 15.21f)
                curveTo(19.642f, 15.21f, 18.708f, 14.752f, 18f, 14f)
                curveTo(17.292f, 14.752f, 16.358f, 15.21f, 15.333f, 15.21f)
                curveTo(15.264f, 15.21f, 15.068f, 15.214f, 15f, 15.21f)
                curveTo(15f, 15.21f, 15f, 15.986f, 15f, 16.398f)
                curveTo(15f, 18.612f, 16.275f, 20.472f, 18f, 21f)
                curveTo(19.725f, 20.472f, 21f, 18.612f, 21f, 16.398f)
                curveTo(21f, 15.986f, 21f, 15.21f, 21f, 15.21f)
                close()
            }
        }.build()

        return _PrivacyPolicy!!
    }

@Suppress("ObjectPropertyName")
private var _PrivacyPolicy: ImageVector? = null
