package com.fibelatti.photowidget.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.model.isWidgetRemoved
import com.fibelatti.photowidget.platform.letIf
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.MyWidgetBadge
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.TrashClock
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MyWidgetsScreen(
    widgets: List<Pair<Int, PhotoWidget>>,
    onWidgetClick: (id: Int, PhotoWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val options: List<PhotoWidgetSource?> = listOf(null) + PhotoWidgetSource.entries
        var selectedSource: PhotoWidgetSource? by rememberSaveable { mutableStateOf(null) }
        val filteredWidgets: List<Pair<Int, PhotoWidget>> = remember(widgets, selectedSource) {
            widgets.filter { (_, widget) -> selectedSource == null || widget.source == selectedSource }
        }

        val enforcedShape: Shape = RoundedCornerShape(28.dp)

        val isAtLeastMediumWidth: Boolean = currentWindowAdaptiveInfoV2().windowSizeClass
            .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        AnimatedContent(
            targetState = filteredWidgets.isNotEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(300, delayMillis = 90))
                    .plus(scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 90)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(90))
                            .plus(scaleOut(targetScale = 0.92f, animationSpec = tween(90))),
                    )
            },
            label = "MyWidgetsScreen_content",
        ) { hasContent: Boolean ->
            if (hasContent) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(count = if (isAtLeastMediumWidth) 4 else 2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 80.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filteredWidgets, key = { (id, _) -> id }) { (id, widget) ->
                        WidgetGridItem(
                            widget = widget,
                            onClick = { onWidgetClick(id, widget) },
                            enforcedShape = enforcedShape,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 120.dp, end = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ColoredShape(
                        shapeId = remember { PhotoWidgetShapeBuilder.shapes.random().id },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(120.dp),
                    )

                    Text(
                        text = stringResource(id = R.string.photo_widget_home_empty_widgets),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, source ->
                val weight by animateFloatAsState(
                    targetValue = if (selectedSource == source) 1.5f else 1f,
                )

                ToggleButton(
                    checked = selectedSource == source,
                    onCheckedChange = { selectedSource = source },
                    modifier = Modifier
                        .weight(weight)
                        .semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    AutoSizeText(
                        text = stringResource(source?.label ?: R.string.photo_widget_home_filter_all),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetGridItem(
    widget: PhotoWidget,
    onClick: () -> Unit,
    enforcedShape: Shape,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ShapedPhoto(
            photo = widget.currentPhoto,
            aspectRatio = widget.aspectRatio,
            shapeId = widget.shapeId,
            cornerRadius = widget.cornerRadius,
            modifier = Modifier
                .fillMaxSize()
                .letIf(widget.aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET) {
                    it.clip(enforcedShape)
                },
            colors = widget.colors,
            border = widget.border,
            isLoading = widget.isLoading,
        )

        when {
            widget.status == PhotoWidgetStatus.DRAFT -> {
                MyWidgetBadge(
                    text = stringResource(R.string.photo_widget_home_draft_label),
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            widget.status == PhotoWidgetStatus.LOCKED -> {
                MyWidgetBadge(
                    text = stringResource(R.string.photo_widget_home_locked_label),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            widget.status.isWidgetRemoved -> {
                MyWidgetBadge(
                    text = stringResource(R.string.photo_widget_home_removed_label),
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                    icon = rememberVectorPainter(AppIcons.TrashClock)
                        .takeIf { widget.status == PhotoWidgetStatus.REMOVED },
                )
            }

            widget.status == PhotoWidgetStatus.INVALID -> {
                MyWidgetBadge(
                    text = stringResource(R.string.photo_widget_home_invalid_label),
                    backgroundColor = Color(0xFFFF8A65),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(enforcedShape)
                .indication(interactionSource, LocalIndication.current),
        )
    }
}

// region Previews
@Composable
@PreviewAll
private fun MyWidgetsScreenPreview() {
    ExtendedTheme {
        val allShapeIds = PhotoWidgetShapeBuilder.shapes.map { it.id }
        val opacities = listOf(70f, 85f, 100f)

        MyWidgetsScreen(
            widgets = List(size = 10) { index ->
                val status = when (index) {
                    2 -> PhotoWidgetStatus.REMOVED
                    3 -> PhotoWidgetStatus.KEPT
                    4 -> PhotoWidgetStatus.INVALID
                    else -> PhotoWidgetStatus.ACTIVE
                }

                index to PhotoWidget(
                    photos = listOf(LocalPhoto(photoId = "photo-1")),
                    aspectRatio = when {
                        index % 3 == 0 -> PhotoWidgetAspectRatio.WIDE
                        index % 2 == 0 -> PhotoWidgetAspectRatio.TALL
                        else -> PhotoWidgetAspectRatio.SQUARE
                    },
                    shapeId = allShapeIds.random(),
                    colors = PhotoWidgetColors(opacity = opacities.random()),
                    status = status,
                    deletionTimestamp = if (status == PhotoWidgetStatus.REMOVED) 1 else -1,
                )
            },
            onWidgetClick = { _, _ -> },
        )
    }
}

@Composable
@PreviewAll
private fun MyWidgetsScreenEmptyPreview() {
    ExtendedTheme {
        MyWidgetsScreen(
            widgets = emptyList(),
            onWidgetClick = { _, _ -> },
        )
    }
}
// endregion Previews
