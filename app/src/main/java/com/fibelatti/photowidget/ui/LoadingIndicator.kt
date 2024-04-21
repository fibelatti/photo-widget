package com.fibelatti.photowidget.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.fibelatti.photowidget.R
import com.fibelatti.ui.preview.DevicePreviews
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(ratio = 1f)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "LoadingIndicator_Transition")

        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 180f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_000),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(offsetMillis = 300),
            ),
            label = "LoadingIndicator_Rotation",
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_hourglass),
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize(fraction = 0.6f)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
@DevicePreviews
private fun LoadingIndicatorPreview() {
    ExtendedTheme {
        LoadingIndicator()
    }
}
