package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.SelectionDialogBottomSheet

@Composable
fun DirectorySortingBottomSheet(
    sheetState: AppSheetState,
    onItemClick: (DirectorySorting) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localResources = LocalResources.current

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        title = stringResource(R.string.photo_widget_directory_sort_title),
        options = DirectorySorting.entries,
        optionName = { localResources.getString(it.label) },
        onOptionSelected = onItemClick,
        modifier = modifier,
        footer = {
            Text(
                text = stringResource(R.string.photo_widget_directory_sort_explanation),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        },
    )
}
