package com.fibelatti.photowidget.configure

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.home.HomeViewModel
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.ComposeBottomSheetDialog
import com.fibelatti.ui.foundation.conditional
import com.fibelatti.ui.foundation.grayScale

object ExistingWidgetPicker {

    fun show(context: Context, onWidgetSelected: (widgetId: Int) -> Unit) {
        ComposeBottomSheetDialog(context) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val currentWidgets by homeViewModel.currentWidgets.collectAsStateWithLifecycle()

            LaunchedEffect(homeViewModel) {
                homeViewModel.loadCurrentWidgets()
            }

            ExistingWidgetPicker(
                currentWidgets = currentWidgets,
                onWidgetSelected = { id ->
                    onWidgetSelected(id)
                    dismiss()
                },
            )
        }.show()
    }
}

@Composable
private fun ExistingWidgetPicker(
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(currentWidgets, key = { (id, _) -> id }) { (id, widget) ->
                val isRemoved = widget.deletionTimestamp > 0

                ShapedPhoto(
                    photo = widget.currentPhoto,
                    aspectRatio = widget.aspectRatio,
                    shapeId = widget.shapeId,
                    cornerRadius = widget.cornerRadius,
                    opacity = if (isRemoved) 70f else widget.opacity,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onWidgetSelected(id) }
                        .conditional(
                            predicate = isRemoved,
                            ifTrue = { grayScale() },
                        ),
                    borderColorHex = widget.borderColor,
                    borderWidth = widget.borderWidth,
                    isLoading = widget.isLoading,
                )
            }
        }
    }
}
