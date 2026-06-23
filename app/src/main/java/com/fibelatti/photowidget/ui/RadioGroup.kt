package com.fibelatti.photowidget.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibelatti.ui.component.AutoSizeText
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.foundation.Shapes

@Composable
fun <T : Any> RadioGroup(
    items: List<T>,
    itemSelected: (T) -> Boolean,
    onItemClick: (T) -> Unit,
    itemTitle: (T) -> String,
    modifier: Modifier = Modifier,
    itemEnabled: (T) -> Boolean = { true },
    itemDescription: (T) -> String? = { null },
    itemFlag: @Composable RowScope.(T) -> Unit = {},
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for ((index: Int, item: T) in items.withIndex()) {
            RadioGroupItem(
                selected = itemSelected(item),
                onClick = { onItemClick(item) },
                title = itemTitle(item),
                description = itemDescription(item),
                enabled = itemEnabled(item),
                shape = when (index) {
                    0 -> Shapes.TopShape
                    items.lastIndex -> Shapes.BottomShape
                    else -> Shapes.MiddleShape
                },
                flag = { itemFlag(item) },
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
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    flag: @Composable RowScope.() -> Unit = {},
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    )

    MaterialListItem(
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = contentColorFor(backgroundColor),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )

                flag()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight)
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        supportingContent = {
            if (description != null) {
                AutoSizeText(
                    text = description,
                    color = contentColorFor(backgroundColor),
                    minFontSize = 8.sp,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = enabled,
            )
        },
        colors = ListItemDefaults.colors(containerColor = backgroundColor),
    )
}
