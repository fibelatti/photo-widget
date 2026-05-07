package com.fibelatti.photowidget.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Minus
import com.fibelatti.photowidget.ui.icons.Plus
import com.fibelatti.ui.text.AutoSizeText

@Composable
fun NumberSpinner(
    value: Int,
    onIncreaseClick: () -> Unit,
    onDecreaseClick: () -> Unit,
    modifier: Modifier = Modifier,
    upperBound: Int? = null,
    lowerBound: Int? = null,
) {
    val localHapticFeedback: HapticFeedback = LocalHapticFeedback.current
    val zeroCorner = CornerSize(0.dp)

    Row(
        modifier = modifier.height(IntrinsicSize.Max),
    ) {
        FilledTonalIconButton(
            onClick = {
                onDecreaseClick()
                localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            },
            modifier = Modifier.fillMaxHeight(),
            enabled = lowerBound == null || value > lowerBound,
            shape = MaterialTheme.shapes.medium.copy(topEnd = zeroCorner, bottomEnd = zeroCorner),
        ) {
            Icon(imageVector = AppIcons.Minus, contentDescription = null)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically(initialOffsetY = { -200 }) + scaleIn() togetherWith
                            slideOutVertically(targetOffsetY = { 200 }) + scaleOut()
                    } else {
                        slideInVertically(initialOffsetY = { 200 }) + scaleIn() togetherWith
                            slideOutVertically(targetOffsetY = { -200 }) + scaleOut()
                    }
                },
            ) { num: Int ->
                AutoSizeText(
                    text = num.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
            }
        }

        FilledTonalIconButton(
            onClick = {
                onIncreaseClick()
                localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            },
            modifier = Modifier.fillMaxHeight(),
            enabled = upperBound == null || value < upperBound,
            shape = MaterialTheme.shapes.medium.copy(topStart = zeroCorner, bottomStart = zeroCorner),
        ) {
            Icon(imageVector = AppIcons.Plus, contentDescription = null)
        }
    }
}
