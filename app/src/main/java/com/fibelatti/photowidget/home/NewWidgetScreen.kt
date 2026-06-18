package com.fibelatti.photowidget.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.rawAspectRatio
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.ui.ShapesBanner
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.ChevronRight
import com.fibelatti.photowidget.ui.icons.Expand
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.foundation.fadingEdges
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme
import kotlin.math.roundToInt

@Composable
fun NewWidgetScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onHelpClick: () -> Unit,
    showBackgroundRestrictionHint: Boolean,
    onBackgroundRestrictionClick: () -> Unit,
    onDismissWarningClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val localInspectionMode = LocalInspectionMode.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ShapesBanner(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(vertical = 10.dp),
        )

        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .padding(top = 72.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AutoSizeText(
                text = stringResource(id = R.string.photo_widget_home_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.headlineLargeEmphasized,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_instruction),
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            val state = rememberLazyListState()

            AspectRatioPicker(
                onAspectRatioSelect = onCreateNewWidgetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingEdges(scrollState = state, isHorizontal = true),
                state = state,
            )

            TextButton(
                onClick = onHelpClick,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(R.string.photo_widget_home_help))
            }

            if (localInspectionMode || (showBackgroundRestrictionHint && localContext.isBackgroundRestricted())) {
                InformationalPanel(
                    text = stringResource(R.string.restriction_warning_hint),
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clickable(
                            onClick = onBackgroundRestrictionClick,
                            role = Role.Button,
                        ),
                    showActionButton = true,
                    onActionButtonClick = onDismissWarningClick,
                )
            }
        }
    }
}

@Composable
fun AspectRatioPicker(
    onAspectRatioSelect: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(
                items = PhotoWidgetAspectRatio.entries,
                key = { _, item -> item.name },
            ) { index, item ->
                AspectRatioItem(
                    item = item,
                    onClick = { onAspectRatioSelect(item) },
                    shape = when (index) {
                        0 -> Shapes.StartShape
                        PhotoWidgetAspectRatio.entries.lastIndex -> Shapes.EndShape
                        else -> Shapes.MiddleShape
                    },
                    itemRepresentation = {
                        when (item) {
                            PhotoWidgetAspectRatio.SQUARE -> ShapedAspectRatioItemRepresentation()
                            PhotoWidgetAspectRatio.ORIGINAL -> OriginalAspectRatioRepresentation()
                            PhotoWidgetAspectRatio.FILL_WIDGET -> FillAspectRatioRepresentation()
                            else -> DefaultAspectRatioItemRepresentation(item = item)
                        }
                    },
                )
            }
        }

        val verticalOffset: Dp by animateDpAsState(
            targetValue = if (state.canScrollForward) 0.dp else (-40).dp,
            animationSpec = tween(durationMillis = 300),
        )
        val alpha: Float by animateFloatAsState(
            targetValue = if (state.canScrollForward) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
        )

        Row(
            modifier = Modifier
                .align(Alignment.End)
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = verticalOffset.toPx()
                }
                .zIndex(-1f)
                .padding(horizontal = 24.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = Shapes.BottomShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AutoSizeText(
                text = stringResource(R.string.hint_scroll_to_view_more),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            val infiniteTransition = rememberInfiniteTransition(label = "ChevronRight_InfiniteTransition")
            val offsetX: Float by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "ChevronRight_OffsetX",
            )

            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.offset { IntOffset(x = offsetX.dp.toPx().roundToInt(), 0) },
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AspectRatioItem(
    item: PhotoWidgetAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    itemRepresentation: @Composable () -> Unit = {
        DefaultAspectRatioItemRepresentation(item = item)
    },
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .height(240.dp)
            .background(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer)
            .clip(shape = shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            itemRepresentation()
        }

        Text(
            text = stringResource(id = item.label),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(id = item.description),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun DefaultAspectRatioItemRepresentation(
    item: PhotoWidgetAspectRatio,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = item.rawAspectRatio)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
            ),
    )
}

@Composable
private fun ShapedAspectRatioItemRepresentation(
    modifier: Modifier = Modifier,
) {
    ColoredShape(
        shapeId = remember {
            PhotoWidgetShapeBuilder.shapes
                .filterNot { it.id.contains("square") }
                .random().id
        },
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun OriginalAspectRatioRepresentation(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .heightIn(max = 120.dp)
                .aspectRatio(ratio = PhotoWidgetAspectRatio.TALL.rawAspectRatio)
                .background(
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .widthIn(max = 100.dp)
                .aspectRatio(ratio = PhotoWidgetAspectRatio.WIDE.rawAspectRatio)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ),
        )
    }
}

@Composable
private fun FillAspectRatioRepresentation(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = PhotoWidgetAspectRatio.FILL_WIDGET.rawAspectRatio)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
            )
            .padding(8.dp),
    ) {
        Icon(
            imageVector = AppIcons.Expand,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

// region Previews
@PreviewAll
@Composable
private fun NewWidgetScreenPreview() {
    ExtendedTheme {
        NewWidgetScreen(
            onCreateNewWidgetClick = {},
            onHelpClick = {},
            showBackgroundRestrictionHint = true,
            onBackgroundRestrictionClick = {},
            onDismissWarningClick = {},
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}
// endregion Previews
