package com.fibelatti.photowidget.home

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.ui.ShapesBanner
import com.fibelatti.ui.text.AutoSizeText

@Composable
fun NewWidgetScreen(
    onCreateNewWidgetClick: (PhotoWidgetAspectRatio) -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(id = R.string.photo_widget_home_aspect_ratio),
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            var selectedAspectRatio by remember {
                mutableStateOf(PhotoWidgetAspectRatio.SQUARE)
            }

            AspectRatioPicker(
                selectedAspectRatio = selectedAspectRatio,
                onAspectRatioSelected = { selectedAspectRatio = it },
                modifier = Modifier.fillMaxWidth(),
            )

            FilledTonalButton(
                onClick = { onCreateNewWidgetClick(selectedAspectRatio) },
            ) {
                Text(text = stringResource(id = R.string.photo_widget_home_new_widget))
            }

            TextButton(
                onClick = onHelpClick,
            ) {
                Text(text = stringResource(R.string.photo_widget_home_help))
            }
        }
    }
}

@Composable
private fun AspectRatioPicker(
    selectedAspectRatio: PhotoWidgetAspectRatio,
    onAspectRatioSelected: (PhotoWidgetAspectRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val listState = rememberLazyListState()

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(PhotoWidgetAspectRatio.entries) {
                AspectRatioItem(
                    ratio = it.aspectRatio,
                    label = stringResource(id = it.label),
                    isSelected = it == selectedAspectRatio,
                    onClick = { onAspectRatioSelected(it) },
                )
            }
        }

        LaunchedEffect(key1 = selectedAspectRatio) {
            listState.animateScrollToItem(
                index = PhotoWidgetAspectRatio.entries.indexOf(selectedAspectRatio),
            )
        }
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
            MaterialTheme.colorScheme.surfaceContainer
        }
    }

    ElevatedCard(
        modifier = modifier
            .width(140.dp)
            .height(180.dp)
            .clip(shape = CardDefaults.elevatedShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val aspectRatioColor by transition.animateColor(label = "AspectRatioColor") { selected ->
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
                    .aspectRatio(ratio = ratio)
                    .background(
                        color = aspectRatioColor,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )

            val textColor by transition.animateColor(label = "LabelColor") { selected ->
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            }

            Text(
                text = label,
                modifier = Modifier.heightIn(min = 40.dp),
                color = textColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}
