package com.fibelatti.photowidget.home

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.ColoredShape
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.ui.foundation.dpToPx
import com.fibelatti.ui.preview.LocalePreviews
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun HomeScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onHelpClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapesBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            AutoSizeText(
                text = stringResource(id = R.string.photo_widget_home_title),
                modifier = Modifier.padding(horizontal = 32.dp),
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )

            var selectedAspectRatio by remember {
                mutableStateOf(PhotoWidgetAspectRatio.SQUARE)
            }

            AspectRatioPicker(
                selectedAspectRatio = selectedAspectRatio,
                onAspectRatioSelected = { selectedAspectRatio = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .fillMaxWidth(),
            )

            FilledTonalButton(
                onClick = { onCreateNewWidgetClick(selectedAspectRatio) },
            ) {
                Text(text = stringResource(id = R.string.photo_widget_home_new_widget))
            }

            TextButton(
                onClick = onHelpClick,
            ) {
                Text(
                    text = stringResource(id = R.string.photo_widget_home_help),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HomeScreenFooter(
                onAppearanceClick = onAppearanceClick,
                onColorsClick = onColorsClick,
                onRateClick = onRateClick,
                onShareClick = onShareClick,
                onViewLicensesClick = onViewLicensesClick,
                modifier = Modifier.padding(all = 16.dp),
            )
        }
    }
}

@Composable
private fun ShapesBanner(
    modifier: Modifier = Modifier,
    polygonSize: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(polygonSize),
    ) {
        val polygons = remember {
            PhotoWidgetShapeBuilder.buildAllShapes().values.drop(4)
        }

        val screenWidth = LocalConfiguration.current.screenWidthDp.dp.dpToPx()
        val transition = rememberInfiniteTransition(label = "ShapesBannerTransition")
        val animationDuration = 2_000
        val animationSpec = tween<Float>(
            durationMillis = polygons.size * animationDuration,
            easing = LinearEasing,
        )

        polygons.forEachIndexed { index, roundedPolygon ->
            val translationValue by transition.animateFloat(
                initialValue = screenWidth,
                targetValue = -polygonSize.dpToPx(),
                animationSpec = infiniteRepeatable(
                    animation = animationSpec,
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(offsetMillis = index * animationDuration),
                ),
                label = "ShapesBannerAnimation_Translation",
            )
            val rotationValue by transition.animateFloat(
                initialValue = 0f,
                targetValue = -360f,
                animationSpec = infiniteRepeatable(
                    animation = animationSpec,
                    repeatMode = RepeatMode.Restart,
                ),
                label = "ShapesBannerAnimation_Rotation",
            )

            ColoredShape(
                polygon = roundedPolygon,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(ratio = 1f)
                    .graphicsLayer {
                        translationX = translationValue
                        rotationZ = rotationValue
                    },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AspectRatioPicker(
    selectedAspectRatio: PhotoWidgetAspectRatio,
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            modifier = Modifier.widthIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            PhotoWidgetAspectRatio.entries.forEach {
                AspectRatioItem(
                    ratio = it.aspectRatio,
                    label = stringResource(id = it.label),
                    isSelected = it == selectedAspectRatio,
                    onClick = { onAspectRatioSelected(it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = stringResource(id = R.string.photo_widget_home_aspect_ratio),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AspectRatioItem(
    ratio: Float,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(
        targetState = isSelected,
        label = "AspectRatioItem_Transition",
    )
    val containerColor by transition.animateColor(label = "ContainerColor") { selected ->
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    ElevatedCard(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .clip(shape = CardDefaults.elevatedShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val aspectRatioColor by transition.animateColor(label = "AspectRatioColor") { selected ->
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(ratio = ratio)
                    .background(
                        color = aspectRatioColor,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HomeScreenFooter(
    onAppearanceClick: () -> Unit,
    onColorsClick: () -> Unit,
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val footerActionModifier = Modifier.weight(1f)

            FooterAction(
                icon = R.drawable.ic_appearance,
                label = R.string.photo_widget_home_appearance,
                onClick = onAppearanceClick,
                modifier = footerActionModifier,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                FooterAction(
                    icon = R.drawable.ic_dynamic_color,
                    label = R.string.photo_widget_home_dynamic_colors,
                    onClick = onColorsClick,
                    modifier = footerActionModifier,
                )
            }

            FooterAction(
                icon = R.drawable.ic_rate,
                label = R.string.photo_widget_home_rate,
                onClick = onRateClick,
                modifier = footerActionModifier,
            )

            FooterAction(
                icon = R.drawable.ic_share,
                label = R.string.photo_widget_home_share,
                onClick = onShareClick,
                modifier = footerActionModifier,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )

        Text(
            text = stringResource(id = R.string.photo_widget_home_developer),
            modifier = Modifier.padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )

        Row(
            modifier = Modifier
                .clickable(
                    onClick = onViewLicensesClick,
                    role = Role.Button,
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.photo_widget_home_version, BuildConfig.VERSION_NAME),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )

            Text(
                text = "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_view_licenses),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FooterAction(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            role = Role.Button,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AutoSizeText(
            text = stringResource(id = label),
            maxLines = 1,
            minTextSize = 8.sp,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
@ThemePreviews
@LocalePreviews
private fun HomeScreenPreview() {
    ExtendedTheme {
        HomeScreen(
            onCreateNewWidgetClick = {},
            onHelpClick = {},
            onAppearanceClick = {},
            onColorsClick = {},
            onRateClick = {},
            onShareClick = {},
            onViewLicensesClick = {},
        )
    }
}
