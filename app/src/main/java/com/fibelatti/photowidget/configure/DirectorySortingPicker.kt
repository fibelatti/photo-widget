package com.fibelatti.photowidget.configure

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.platform.SelectionDialog

object DirectorySortingPicker {

    fun show(
        context: Context,
        onItemClick: (DirectorySorting) -> Unit,
    ) {
        SelectionDialog.show(
            context = context,
            title = context.getString(R.string.photo_widget_directory_sort_title),
            options = DirectorySorting.entries,
            optionName = { context.getString(it.label) },
            onOptionSelected = onItemClick,
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
}
