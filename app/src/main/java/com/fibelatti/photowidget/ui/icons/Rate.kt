package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Rate: ImageVector
    get() {
        if (_Rate != null) {
            return _Rate!!
        }
        _Rate = ImageVector.Builder(
            name = "Rate",
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
                moveTo(11.245f, 4.174f)
                curveTo(11.476f, 3.508f, 11.592f, 3.175f, 11.763f, 3.083f)
                curveTo(11.911f, 3.003f, 12.09f, 3.003f, 12.238f, 3.083f)
                curveTo(12.409f, 3.175f, 12.525f, 3.508f, 12.756f, 4.174f)
                lineTo(14.287f, 8.576f)
                curveTo(14.352f, 8.766f, 14.385f, 8.861f, 14.445f, 8.931f)
                curveTo(14.497f, 8.994f, 14.564f, 9.042f, 14.64f, 9.073f)
                curveTo(14.725f, 9.107f, 14.825f, 9.109f, 15.026f, 9.114f)
                lineTo(19.686f, 9.209f)
                curveTo(20.391f, 9.223f, 20.743f, 9.23f, 20.884f, 9.364f)
                curveTo(21.005f, 9.481f, 21.06f, 9.65f, 21.03f, 9.816f)
                curveTo(20.996f, 10.007f, 20.715f, 10.22f, 20.153f, 10.646f)
                lineTo(16.439f, 13.462f)
                curveTo(16.279f, 13.583f, 16.199f, 13.644f, 16.15f, 13.722f)
                curveTo(16.107f, 13.791f, 16.081f, 13.87f, 16.076f, 13.951f)
                curveTo(16.069f, 14.043f, 16.098f, 14.139f, 16.156f, 14.331f)
                lineTo(17.506f, 18.792f)
                curveTo(17.71f, 19.467f, 17.812f, 19.804f, 17.728f, 19.979f)
                curveTo(17.655f, 20.131f, 17.511f, 20.236f, 17.344f, 20.258f)
                curveTo(17.151f, 20.284f, 16.862f, 20.083f, 16.283f, 19.68f)
                lineTo(12.458f, 17.018f)
                curveTo(12.293f, 16.903f, 12.211f, 16.846f, 12.121f, 16.824f)
                curveTo(12.042f, 16.804f, 11.959f, 16.804f, 11.88f, 16.824f)
                curveTo(11.791f, 16.846f, 11.708f, 16.903f, 11.544f, 17.018f)
                lineTo(7.718f, 19.68f)
                curveTo(7.139f, 20.083f, 6.85f, 20.284f, 6.657f, 20.258f)
                curveTo(6.491f, 20.236f, 6.346f, 20.131f, 6.273f, 19.979f)
                curveTo(6.189f, 19.804f, 6.291f, 19.467f, 6.495f, 18.792f)
                lineTo(7.845f, 14.331f)
                curveTo(7.903f, 14.139f, 7.932f, 14.043f, 7.926f, 13.951f)
                curveTo(7.92f, 13.87f, 7.894f, 13.791f, 7.851f, 13.722f)
                curveTo(7.802f, 13.644f, 7.723f, 13.583f, 7.563f, 13.462f)
                lineTo(3.849f, 10.646f)
                curveTo(3.287f, 10.22f, 3.006f, 10.007f, 2.971f, 9.816f)
                curveTo(2.941f, 9.65f, 2.996f, 9.481f, 3.118f, 9.364f)
                curveTo(3.258f, 9.23f, 3.611f, 9.223f, 4.316f, 9.209f)
                lineTo(8.975f, 9.114f)
                curveTo(9.176f, 9.109f, 9.276f, 9.107f, 9.362f, 9.073f)
                curveTo(9.437f, 9.042f, 9.504f, 8.994f, 9.557f, 8.931f)
                curveTo(9.616f, 8.861f, 9.649f, 8.766f, 9.715f, 8.576f)
                lineTo(11.245f, 4.174f)
                close()
            }
        }.build()

        return _Rate!!
    }

@Suppress("ObjectPropertyName")
private var _Rate: ImageVector? = null
