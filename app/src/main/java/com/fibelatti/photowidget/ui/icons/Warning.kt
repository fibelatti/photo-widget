package com.fibelatti.photowidget.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val AppIcons.Warning: ImageVector
    get() {
        _Warning?.let { return it }

        return ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                horizontalLineTo(12.01f)
                moveTo(12f, 10f)
                verticalLineTo(14f)
                moveTo(6.412f, 21f)
                horizontalLineTo(17.588f)
                curveTo(19.37f, 21f, 20.26f, 21f, 20.783f, 20.625f)
                curveTo(21.239f, 20.299f, 21.537f, 19.795f, 21.603f, 19.238f)
                curveTo(21.68f, 18.6f, 21.25f, 17.819f, 20.392f, 16.258f)
                lineTo(14.804f, 6.098f)
                curveTo(13.89f, 4.436f, 13.433f, 3.605f, 12.829f, 3.33f)
                curveTo(12.302f, 3.09f, 11.698f, 3.09f, 11.171f, 3.33f)
                curveTo(10.567f, 3.605f, 10.11f, 4.436f, 9.196f, 6.098f)
                lineTo(3.608f, 16.258f)
                curveTo(2.75f, 17.819f, 2.32f, 18.6f, 2.397f, 19.238f)
                curveTo(2.464f, 19.795f, 2.761f, 20.299f, 3.217f, 20.625f)
                curveTo(3.74f, 21f, 4.63f, 21f, 6.412f, 21f)
                close()
            }
        }.build().also { _Warning = it }
    }

@Suppress("ObjectPropertyName")
private var _Warning: ImageVector? = null
