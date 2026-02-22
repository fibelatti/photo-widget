package com.fibelatti.photowidget.configure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.home.HomeViewModel
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.model.isWidgetRemoved
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.ui.MyWidgetBadge
import com.fibelatti.photowidget.ui.ShapedPhoto
import com.fibelatti.ui.foundation.AppBottomSheet
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.conditional
import com.fibelatti.ui.foundation.hideBottomSheet

@Composable
fun ImportFromWidgetBottomSheet(
    sheetState: AppSheetState,
    onWidgetSelected: (appWidgetId: Int) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        val currentWidgets by homeViewModel.currentWidgets.collectAsStateWithLifecycle()

        RememberedEffect(homeViewModel) {
            homeViewModel.loadWidgets()
        }

        ImportFromWidgetContent(
            currentWidgets = currentWidgets,
            onWidgetSelected = { id ->
                onWidgetSelected(id)
                sheetState.hideBottomSheet()
            },
        )
    }
}

@Composable
private fun ImportFromWidgetContent(
    currentWidgets: List<Pair<Int, PhotoWidget>>,
    onWidgetSelected: (widgetId: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .nestedScroll(rememberNestedScrollInteropConnection()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_widget_configure_import_dialog_title),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = stringResource(R.string.photo_widget_configure_import_dialog_description),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
        )

        val enforcedShape: Shape = RoundedCornerShape(28.dp)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(currentWidgets, key = { (id, _) -> id }) { (id, widget) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onWidgetSelected(id) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    ShapedPhoto(
                        photo = widget.currentPhoto,
                        aspectRatio = widget.aspectRatio,
                        shapeId = widget.shapeId,
                        cornerRadius = widget.cornerRadius,
                        modifier = Modifier
                            .fillMaxSize()
                            .conditional(
                                predicate = widget.aspectRatio == PhotoWidgetAspectRatio.FILL_WIDGET,
                                ifTrue = { clip(enforcedShape) },
                            ),
                        colors = widget.colors,
                        border = widget.border,
                        isLoading = widget.isLoading,
                    )

                    if (widget.status.isWidgetRemoved) {
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
}
