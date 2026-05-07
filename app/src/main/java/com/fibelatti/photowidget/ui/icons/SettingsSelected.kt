package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.SettingsSelected: ImageVector
    get() {
        if (_SettingsSelected != null) {
            return _SettingsSelected!!
        }
        _SettingsSelected = ImageVector.Builder(
            name = "SettingsSelected",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.905f, 3.06f)
                curveTo(12.699f, 3f, 12.466f, 3f, 12f, 3f)
                curveTo(11.534f, 3f, 11.301f, 3f, 11.095f, 3.06f)
                curveTo(10.794f, 3.148f, 10.528f, 3.328f, 10.335f, 3.575f)
                curveTo(10.202f, 3.744f, 10.116f, 3.96f, 9.943f, 4.393f)
                curveTo(9.694f, 5.015f, 9.004f, 5.335f, 8.369f, 5.123f)
                lineTo(7.798f, 4.933f)
                curveTo(7.393f, 4.798f, 7.19f, 4.73f, 6.992f, 4.719f)
                curveTo(6.7f, 4.702f, 6.41f, 4.77f, 6.157f, 4.916f)
                curveTo(5.985f, 5.015f, 5.834f, 5.166f, 5.532f, 5.468f)
                curveTo(5.211f, 5.788f, 5.051f, 5.949f, 4.949f, 6.132f)
                curveTo(4.799f, 6.401f, 4.736f, 6.709f, 4.768f, 7.016f)
                curveTo(4.789f, 7.224f, 4.873f, 7.434f, 5.042f, 7.856f)
                curveTo(5.306f, 8.515f, 5.052f, 9.269f, 4.443f, 9.634f)
                lineTo(4.165f, 9.801f)
                curveTo(3.74f, 10.056f, 3.528f, 10.183f, 3.374f, 10.359f)
                curveTo(3.237f, 10.514f, 3.134f, 10.696f, 3.071f, 10.893f)
                curveTo(3f, 11.116f, 3f, 11.366f, 3f, 11.866f)
                curveTo(3f, 12.459f, 3f, 12.755f, 3.095f, 13.009f)
                curveTo(3.178f, 13.233f, 3.314f, 13.434f, 3.491f, 13.595f)
                curveTo(3.692f, 13.777f, 3.964f, 13.886f, 4.509f, 14.104f)
                curveTo(5.065f, 14.326f, 5.352f, 14.944f, 5.162f, 15.513f)
                lineTo(4.947f, 16.158f)
                curveTo(4.798f, 16.605f, 4.724f, 16.829f, 4.717f, 17.049f)
                curveTo(4.709f, 17.313f, 4.77f, 17.574f, 4.896f, 17.807f)
                curveTo(5f, 18f, 5.167f, 18.167f, 5.5f, 18.5f)
                curveTo(5.833f, 18.833f, 6f, 19f, 6.193f, 19.104f)
                curveTo(6.426f, 19.229f, 6.687f, 19.291f, 6.951f, 19.283f)
                curveTo(7.171f, 19.276f, 7.395f, 19.202f, 7.842f, 19.053f)
                lineTo(8.369f, 18.877f)
                curveTo(9.004f, 18.665f, 9.694f, 18.986f, 9.943f, 19.607f)
                curveTo(10.116f, 20.04f, 10.202f, 20.256f, 10.335f, 20.425f)
                curveTo(10.528f, 20.672f, 10.794f, 20.852f, 11.095f, 20.94f)
                curveTo(11.301f, 21f, 11.534f, 21f, 12f, 21f)
                curveTo(12.466f, 21f, 12.699f, 21f, 12.905f, 20.94f)
                curveTo(13.206f, 20.852f, 13.472f, 20.672f, 13.665f, 20.425f)
                curveTo(13.798f, 20.256f, 13.884f, 20.04f, 14.057f, 19.607f)
                curveTo(14.306f, 18.986f, 14.996f, 18.665f, 15.631f, 18.877f)
                lineTo(16.158f, 19.053f)
                curveTo(16.605f, 19.202f, 16.829f, 19.276f, 17.048f, 19.283f)
                curveTo(17.312f, 19.291f, 17.574f, 19.23f, 17.806f, 19.104f)
                curveTo(18f, 19f, 18.166f, 18.833f, 18.5f, 18.5f)
                curveTo(18.833f, 18.167f, 18.999f, 18f, 19.104f, 17.807f)
                curveTo(19.229f, 17.574f, 19.291f, 17.313f, 19.283f, 17.049f)
                curveTo(19.276f, 16.829f, 19.201f, 16.605f, 19.052f, 16.158f)
                lineTo(18.837f, 15.513f)
                curveTo(18.648f, 14.944f, 18.934f, 14.326f, 19.491f, 14.104f)
                curveTo(20.036f, 13.886f, 20.308f, 13.777f, 20.509f, 13.595f)
                curveTo(20.686f, 13.434f, 20.822f, 13.233f, 20.905f, 13.009f)
                curveTo(21f, 12.755f, 21f, 12.459f, 21f, 11.866f)
                curveTo(21f, 11.366f, 21f, 11.116f, 20.929f, 10.893f)
                curveTo(20.866f, 10.696f, 20.763f, 10.514f, 20.626f, 10.359f)
                curveTo(20.472f, 10.183f, 20.26f, 10.056f, 19.835f, 9.801f)
                lineTo(19.557f, 9.634f)
                curveTo(18.948f, 9.269f, 18.694f, 8.515f, 18.958f, 7.856f)
                curveTo(19.126f, 7.434f, 19.211f, 7.224f, 19.232f, 7.015f)
                curveTo(19.264f, 6.709f, 19.2f, 6.401f, 19.051f, 6.132f)
                curveTo(18.949f, 5.949f, 18.788f, 5.788f, 18.468f, 5.468f)
                curveTo(18.166f, 5.166f, 18.015f, 5.015f, 17.843f, 4.916f)
                curveTo(17.589f, 4.77f, 17.299f, 4.702f, 17.008f, 4.719f)
                curveTo(16.809f, 4.73f, 16.607f, 4.798f, 16.202f, 4.933f)
                lineTo(15.631f, 5.123f)
                curveTo(14.996f, 5.335f, 14.306f, 5.015f, 14.057f, 4.393f)
                curveTo(13.884f, 3.96f, 13.798f, 3.744f, 13.665f, 3.575f)
                curveTo(13.472f, 3.328f, 13.206f, 3.148f, 12.905f, 3.06f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 12f)
                curveTo(15f, 13.657f, 13.657f, 15f, 12f, 15f)
                curveTo(10.343f, 15f, 9f, 13.657f, 9f, 12f)
                curveTo(9f, 10.343f, 10.343f, 9f, 12f, 9f)
                curveTo(13.657f, 9f, 15f, 10.343f, 15f, 12f)
                close()
            }
        }.build()

        return _SettingsSelected!!
    }

@Suppress("ObjectPropertyName")
private var _SettingsSelected: ImageVector? = null
