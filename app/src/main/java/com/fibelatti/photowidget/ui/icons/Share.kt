package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Share: ImageVector
    get() {
        if (_Share != null) {
            return _Share!!
        }
        _Share = ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(680.94f, 888.13f)
                quadToRelative(-53.09f, 0f, -90.24f, -37.09f)
                quadToRelative(-37.16f, -37.09f, -37.16f, -90.08f)
                quadToRelative(0f, -5.76f, 2.76f, -25.85f)
                lineTo(283.43f, 576.13f)
                quadToRelative(-16.71f, 14.76f, -38.17f, 23.14f)
                reflectiveQuadToRelative(-45.98f, 8.38f)
                quadToRelative(-53.09f, 0f, -90.25f, -37.21f)
                quadToRelative(-37.16f, -37.22f, -37.16f, -90.38f)
                quadToRelative(0f, -53.17f, 37.16f, -90.44f)
                quadToRelative(37.16f, -37.27f, 90.25f, -37.27f)
                quadToRelative(24.48f, 0f, 46.2f, 8.5f)
                quadToRelative(21.72f, 8.5f, 38.67f, 23.5f)
                lineToRelative(271.92f, -158.5f)
                quadToRelative(-1.77f, -6.52f, -2.15f, -12.9f)
                quadToRelative(-0.38f, -6.38f, -0.38f, -13.67f)
                quadToRelative(0f, -53.09f, 37.17f, -90.25f)
                reflectiveQuadToRelative(90.26f, -37.16f)
                quadToRelative(53.1f, 0f, 90.25f, 37.17f)
                quadToRelative(37.15f, 37.16f, 37.15f, 90.26f)
                quadToRelative(0f, 53.09f, -37.16f, 90.24f)
                quadToRelative(-37.16f, 37.16f, -90.25f, 37.16f)
                quadToRelative(-25.05f, 0f, -46.96f, -8.74f)
                reflectiveQuadToRelative(-38.87f, -24.22f)
                lineTo(324.17f, 451.28f)
                quadToRelative(2f, 7.25f, 2.5f, 13.99f)
                quadToRelative(0.5f, 6.73f, 0.5f, 14.85f)
                reflectiveQuadToRelative(-0.62f, 15.1f)
                quadToRelative(-0.62f, 6.98f, -2.62f, 14.21f)
                lineToRelative(270.72f, 157.55f)
                quadToRelative(16.96f, -15.72f, 39.02f, -24.7f)
                quadToRelative(22.07f, -8.98f, 47.29f, -8.98f)
                quadToRelative(53.09f, 0f, 90.25f, 37.22f)
                reflectiveQuadToRelative(37.16f, 90.38f)
                quadToRelative(0f, 53.17f, -37.17f, 90.2f)
                quadToRelative(-37.16f, 37.03f, -90.26f, 37.03f)
                close()
            }
        }.build()

        return _Share!!
    }

@Suppress("ObjectPropertyName")
private var _Share: ImageVector? = null
