package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Album: ImageVector
    get() {
        if (_Album != null) {
            return _Album!!
        }
        _Album = ImageVector.Builder(
            name = "Album",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(123.59f, 768.13f)
                quadToRelative(-37.79f, 0f, -64.39f, -26.61f)
                quadToRelative(-26.61f, -26.61f, -26.61f, -64.39f)
                verticalLineToRelative(-394.26f)
                quadToRelative(0f, -37.78f, 26.61f, -64.39f)
                quadToRelative(26.6f, -26.61f, 64.39f, -26.61f)
                horizontalLineToRelative(394.26f)
                quadToRelative(37.78f, 0f, 64.39f, 26.61f)
                reflectiveQuadToRelative(26.61f, 64.39f)
                verticalLineToRelative(394.26f)
                quadToRelative(0f, 37.78f, -26.61f, 64.39f)
                reflectiveQuadToRelative(-64.39f, 26.61f)
                lineTo(123.59f, 768.13f)
                close()
                moveTo(300.72f, 560f)
                lineTo(260f, 506.04f)
                quadToRelative(-5.76f, -7.52f, -15.28f, -7.14f)
                quadToRelative(-9.52f, 0.38f, -15.29f, 7.9f)
                lineToRelative(-46.91f, 62.87f)
                quadToRelative(-7.52f, 9.53f, -1.5f, 19.93f)
                reflectiveQuadToRelative(17.55f, 10.4f)
                horizontalLineToRelative(244.3f)
                quadToRelative(11.52f, 0f, 17.04f, -10.4f)
                reflectiveQuadToRelative(-2f, -19.93f)
                lineToRelative(-66.15f, -89.63f)
                quadToRelative(-5.76f, -7.52f, -15.16f, -7.52f)
                reflectiveQuadToRelative(-15.17f, 7.52f)
                lineTo(300.72f, 560f)
                close()
                moveTo(726.22f, 768.13f)
                quadToRelative(-17.72f, 0f, -29.82f, -12.1f)
                quadToRelative(-12.1f, -12.1f, -12.1f, -29.81f)
                verticalLineToRelative(-492.44f)
                quadToRelative(0f, -17.71f, 12.1f, -29.81f)
                quadToRelative(12.1f, -12.1f, 29.82f, -12.1f)
                quadToRelative(17.71f, 0f, 29.81f, 12.1f)
                quadToRelative(12.1f, 12.1f, 12.1f, 29.81f)
                verticalLineToRelative(492.44f)
                quadToRelative(0f, 17.71f, -12.1f, 29.81f)
                quadToRelative(-12.1f, 12.1f, -29.81f, 12.1f)
                close()
                moveTo(885.5f, 768.13f)
                quadToRelative(-17.72f, 0f, -29.82f, -12.1f)
                quadToRelative(-12.09f, -12.1f, -12.09f, -29.81f)
                verticalLineToRelative(-492.44f)
                quadToRelative(0f, -17.71f, 12.09f, -29.81f)
                quadToRelative(12.1f, -12.1f, 29.82f, -12.1f)
                reflectiveQuadToRelative(29.82f, 12.1f)
                quadToRelative(12.09f, 12.1f, 12.09f, 29.81f)
                verticalLineToRelative(492.44f)
                quadToRelative(0f, 17.71f, -12.09f, 29.81f)
                quadToRelative(-12.1f, 12.1f, -29.82f, 12.1f)
                close()
            }
        }.build()

        return _Album!!
    }

@Suppress("ObjectPropertyName")
private var _Album: ImageVector? = null
