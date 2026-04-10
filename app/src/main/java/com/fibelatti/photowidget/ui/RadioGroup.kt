package com.fibelatti.photowidget.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.foundation.Shapes

@Composable
fun <T : Any> RadioGroup(
    items: List<T>,
    itemSelected: (T) -> Boolean,
    onItemClick: (T) -> Unit,
    itemTitle: (T) -> String,
    modifier: Modifier = Modifier,
    itemDescription: (T) -> String? = { null },
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
                shape = when (index) {
                    0 -> Shapes.TopShape
                    items.lastIndex -> Shapes.BottomShape
                    else -> Shapes.MiddleShape
                },
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
    shape: Shape = MaterialTheme.shapes.small,
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(color = backgroundColor, shape = shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .clip(shape)
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
