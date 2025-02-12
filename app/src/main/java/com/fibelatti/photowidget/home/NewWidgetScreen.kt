package com.fibelatti.photowidget.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import com.fibelatti.photowidget.ui.ShapesBanner
import com.fibelatti.ui.preview.AllPreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

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
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(top = 68.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AutoSizeText(
                text = stringResource(id = R.string.photo_widget_home_title),
                modifier = Modifier.padding(horizontal = 32.dp),
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(id = R.string.photo_widget_home_instruction),
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            AspectRatioPicker(
                onAspectRatioSelected = onCreateNewWidgetClick,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = onHelpClick,
            ) {
                Text(text = stringResource(R.string.photo_widget_home_help))
            }

            if (localInspectionMode || (showBackgroundRestrictionHint && localContext.isBackgroundRestricted())) {
                BackgroundRestrictionWarning(
                    onClick = onBackgroundRestrictionClick,
                    modifier = Modifier.widthIn(max = 300.dp),
                    showDismissButton = true,
                    onDismissClick = onDismissWarningClick,
                )
            }
        }
    }
}

@Composable
private fun AspectRatioPicker(
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(PhotoWidgetAspectRatio.entries) { item ->
            AspectRatioItem(
                item = item,
                onClick = { onAspectRatioSelected(item) },
                itemRepresentation = {
                    when (item) {
                        PhotoWidgetAspectRatio.SQUARE, PhotoWidgetAspectRatio.TALL, PhotoWidgetAspectRatio.WIDE -> {
                            DefaultAspectRatioItemRepresentation(item = item)
                        }

                        PhotoWidgetAspectRatio.ORIGINAL -> {
                            OriginalAspectRatioRepresentation()
                        }

                        PhotoWidgetAspectRatio.FILL_WIDGET -> {
                            FillAspectRatioRepresentation()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AspectRatioItem(
    item: PhotoWidgetAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemRepresentation: @Composable () -> Unit = {
        DefaultAspectRatioItemRepresentation(item = item)
    },
) {
    ElevatedCard(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .clip(shape = CardDefaults.elevatedShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                itemRepresentation()
            }

            Text(
                text = stringResource(id = item.label),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(id = item.description),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun DefaultAspectRatioItemRepresentation(
    item: PhotoWidgetAspectRatio,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(ratio = item.aspectRatio)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
            ),
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
                .heightIn(max = 80.dp)
                .aspectRatio(ratio = PhotoWidgetAspectRatio.TALL.aspectRatio)
                .background(
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .widthIn(max = 80.dp)
                .aspectRatio(ratio = PhotoWidgetAspectRatio.WIDE.aspectRatio)
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
            .aspectRatio(ratio = PhotoWidgetAspectRatio.FILL_WIDGET.aspectRatio)
            .background(
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small,
            )
            .padding(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_expand),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
fun BackgroundRestrictionWarning(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDismissButton: Boolean = false,
    onDismissClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFFFFE57F),
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(
                onClick = onClick,
                role = Role.Button,
            )
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
            )

            Text(
                text = stringResource(R.string.restriction_warning_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (showDismissButton) {
            Text(
                text = stringResource(R.string.photo_widget_action_got_it),
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(
                        onClick = onDismissClick,
                        role = Role.Button,
                    ),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// region Previews
@AllPreviews
@Composable
private fun NewWidgetScreenPreview() {
    ExtendedTheme {
        NewWidgetScreen(
            onCreateNewWidgetClick = {},
            onHelpClick = {},
            showBackgroundRestrictionHint = true,
            onBackgroundRestrictionClick = {},
            onDismissWarningClick = {},
        )
    }
}
// endregion Previews
