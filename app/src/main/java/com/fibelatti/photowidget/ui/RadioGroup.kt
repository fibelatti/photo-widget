package com.fibelatti.photowidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun <T : Any> RadioGroup(
    items: List<T>,
    itemSelected: (T) -> Boolean,
    onItemClick: (T) -> Unit,
    itemTitle: (T) -> String,
    itemDescription: (T) -> String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (item in items) {
            RadioGroupItem(
                selected = itemSelected(item),
                onClick = { onItemClick(item) },
                title = itemTitle(item),
                description = itemDescription(item),
                backgroundColor = backgroundColor,
                outlineColor = outlineColor,
                shape = shape,
            )
        }
    }
}

@Composable
private fun RadioGroupItem(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = outlineColor,
                shape = shape,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                color = contentColorFor(backgroundColor),
                style = MaterialTheme.typography.titleMedium,
            )

            if (description != null) {
                Text(
                    text = description,
                    color = contentColorFor(backgroundColor),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
