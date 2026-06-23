package com.fibelatti.photowidget.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Check
import com.fibelatti.photowidget.ui.icons.Xmark
import com.fibelatti.ui.component.ListItem
import com.fibelatti.ui.foundation.Shapes

@Composable
fun BooleanListItem(
    title: String,
    currentValue: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.StandaloneShape,
) {
    ListItem(
        headlineText = title,
        trailingContent = {
            Switch(
                checked = currentValue,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    val icon: ImageVector = if (currentValue) AppIcons.Check else AppIcons.Xmark

                    AnimatedContent(
                        targetState = icon,
                        transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                    ) { vector ->
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
            )
        },
        modifier = modifier,
        shape = shape,
    )
}

@Composable
fun PickerListItem(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.StandaloneShape,
) {
    ListItem(
        headlineText = title,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        supportingText = currentValue,
        shape = shape,
    )
}

@Composable
fun ShapeListItem(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.StandaloneShape,
) {
    ListItem(
        headlineText = title,
        trailingContent = {
            ColoredShape(
                shapeId = currentValue,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        },
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
    )
}
