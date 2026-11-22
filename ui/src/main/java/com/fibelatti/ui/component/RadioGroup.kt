package com.fibelatti.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> RadioGroup(
    items: List<T>,
    itemSelected: (T) -> Boolean,
    onItemClick: (T) -> Unit,
    itemTitle: (T) -> String,
    modifier: Modifier = Modifier,
    itemEnabled: (T) -> Boolean = { true },
    itemDescription: (T) -> String? = { null },
    itemFlag: @Composable RowScope.(T) -> Unit = {},
) {
    SegmentedColumn(modifier = modifier.selectableGroup()) {
        items.forEach { option: T ->
            item { shape: Shape ->
                RadioGroupItem(
                    selected = itemSelected(option),
                    onClick = { onItemClick(option) },
                    title = itemTitle(option),
                    description = itemDescription(option),
                    enabled = itemEnabled(option),
                    shape = shape,
                    flag = { itemFlag(option) },
                )
            }
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
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    flag: @Composable RowScope.() -> Unit = {},
) {
    ListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight),
        enabled = enabled,
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        supportingContent = {
            if (description != null) {
                AutoSizeText(
                    text = description,
                    minFontSize = 8.sp,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        shapes = ListItemDefaults.shapes(shape = shape),
        colors = selectableListItemColors(),
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )

                flag()
            }
        },
    )
}
