package com.fibelatti.photowidget.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetColors
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.model.isWidgetRemoved
import com.fibelatti.photowidget.model.photoCycleEnabled
import com.fibelatti.photowidget.ui.ColoredShape
import com.fibelatti.photowidget.ui.MyWidgetBadge
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.photowidget.ui.WarningSign
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
fun MyWidgetsScreen(
    widgets: List<Pair<Int, PhotoWidget>>,
    onCurrentWidgetClick: (appWidgetId: Int, canSync: Boolean, canLock: Boolean, isLocked: Boolean) -> Unit,
    onRemovedWidgetClick: (appWidgetId: Int, PhotoWidgetStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val maxWidth = maxWidth

        val options = listOf(null, PhotoWidgetSource.PHOTOS, PhotoWidgetSource.DIRECTORY)
        var selectedSource: PhotoWidgetSource? by remember { mutableStateOf(null) }
        val filteredWidgets: List<Pair<Int, PhotoWidget>> by remember(widgets) {
            derivedStateOf {
                widgets.filter { selectedSource == null || it.second.source == selectedSource }
            }
        }
        val hasDeletedWidgets = remember(widgets) {
            filteredWidgets.any { it.second.status.isWidgetRemoved }
        }

        AnimatedContent(
            targetState = filteredWidgets,
            transitionSpec = {
                fadeIn(animationSpec = tween(300, delayMillis = 90))
                    .plus(scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 90)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(90))
                            .plus(scaleOut(targetScale = 0.92f, animationSpec = tween(90))),
                    )
            },
            label = "MyWidgetsScreen_content",
        ) { items ->
            if (items.isNotEmpty()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(count = if (maxWidth < 600.dp) 2 else 4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 80.dp, end = 16.dp, bottom = 120.dp),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items, key = { (id, _) -> id }) { (id, widget) ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (widget.status.isWidgetRemoved) {
                                        onRemovedWidgetClick(id, widget.status)
                                    } else {
                                        onCurrentWidgetClick(
                                            /* appWidgetId = */ id,
                                            /* canSync = */ widget.source == PhotoWidgetSource.DIRECTORY,
                                            /* canLock = */ widget.photos.isNotEmpty(),
                                            /* isLocked = */ PhotoWidgetStatus.LOCKED == widget.status,
                                        )
                                    }
                                },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            ShapedPhoto(
                                photo = widget.currentPhoto,
                                aspectRatio = widget.aspectRatio,
                                shapeId = widget.shapeId,
                                cornerRadius = widget.cornerRadius,
                                modifier = Modifier.fillMaxSize(),
                                colors = widget.colors,
                                border = widget.border,
                                isLoading = widget.isLoading,
                            )

                            when {
                                PhotoWidgetStatus.LOCKED == widget.status -> {
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
                                        icon = painterResource(R.drawable.ic_trash_clock)
                                            .takeIf { PhotoWidgetStatus.REMOVED == widget.status },
                                    )
                                }
                            }
                        }
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

        if (hasDeletedWidgets) {
            WarningSign(
                text = stringResource(id = R.string.photo_widget_home_removed_widgets_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp, bottom = 16.dp, end = 32.dp),
            )
        }
    }
}

// region Previews
@Composable
@AllPreviews
private fun MyWidgetsScreenPreview() {
    ExtendedTheme {
        val allShapeIds = PhotoWidgetShapeBuilder.shapes.map { it.id }
        val opacities = listOf(70f, 85f, 100f)

        MyWidgetsScreen(
            widgets = List(size = 10) { index ->
                val status = when (index) {
                    2 -> PhotoWidgetStatus.REMOVED
                    3 -> PhotoWidgetStatus.KEPT
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
                    deletionTimestamp = if (PhotoWidgetStatus.REMOVED == status) 1 else -1,
                )
            },
            onCurrentWidgetClick = { _, _, _, _ -> },
            onRemovedWidgetClick = { _, _ -> },
        )
    }
}

@Composable
@AllPreviews
private fun MyWidgetsScreenEmptyPreview() {
    ExtendedTheme {
        MyWidgetsScreen(
            widgets = emptyList(),
            onCurrentWidgetClick = { _, _, _, _ -> },
            onRemovedWidgetClick = { _, _ -> },
        )
    }
}
// endregion Previews
