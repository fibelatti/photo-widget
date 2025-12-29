package com.fibelatti.photowidget.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.textToBitmap
import com.fibelatti.photowidget.platform.withRoundedCorners
import com.fibelatti.ui.foundation.dpToPx
import kotlin.math.abs

@Composable
fun WidgetPositionViewer(
    photoWidget: PhotoWidget,
    modifier: Modifier = Modifier,
    areaColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val areaRadius: Float = 28.dp.dpToPx()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawRoundRect(
                    color = areaColor,
                    cornerRadius = CornerRadius(areaRadius),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    ),
                )
                drawContent()
            }
            .clip(shape = RoundedCornerShape(size = areaRadius)),
        contentAlignment = Alignment.Center,
    ) {
        val verticalOffset: Dp = abs(photoWidget.verticalOffset).dp * PhotoWidget.POSITIONING_MULTIPLIER
        val horizontalOffset: Dp = abs(photoWidget.horizontalOffset).dp * PhotoWidget.POSITIONING_MULTIPLIER

        Box(
            modifier = Modifier
                .padding(
                    start = if (photoWidget.horizontalOffset > 0) horizontalOffset else 0.dp,
                    top = if (photoWidget.verticalOffset > 0) verticalOffset else 0.dp,
                    end = if (photoWidget.horizontalOffset < 0) horizontalOffset else 0.dp,
                    bottom = if (photoWidget.verticalOffset < 0) verticalOffset else 0.dp,
                )
                .padding(all = photoWidget.padding.dp * PhotoWidget.POSITIONING_MULTIPLIER),
            contentAlignment = Alignment.Center,
        ) {
            if (photoWidget.currentPhoto != null) {
                ShapedPhoto(
                    photo = photoWidget.currentPhoto,
                    aspectRatio = photoWidget.aspectRatio,
                    shapeId = photoWidget.shapeId,
                    cornerRadius = photoWidget.cornerRadius,
                    colors = photoWidget.colors,
                    border = photoWidget.border,
                )
            } else {
                Image(
                    bitmap = rememberSampleBitmap()
                        .withRoundedCorners(radius = PhotoWidget.DEFAULT_CORNER_RADIUS.dpToPx())
                        .asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        if (photoWidget.text is PhotoWidgetText.Label) {
            Image(
                bitmap = photoWidget.text
                    .textToBitmap(context = LocalContext.current)
                    .asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = abs(photoWidget.text.verticalOffset).dp),
            )
        }
    }
}
