package com.fibelatti.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.icons.AppIcons
import com.fibelatti.ui.icons.Check
import com.fibelatti.ui.icons.Xmark

object ListItem {

    val MinHeight: Dp = 72.dp

    val DefaultShape: Shape = Shapes.StandaloneShape
}

@Composable
fun ListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    shape: Shape = ListItem.DefaultShape,
    headlineFlag: @Composable RowScope.() -> Unit = {},
) {
    androidx.compose.material3.ListItem(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight)
            .clip(shape),
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = {
            if (!supportingText.isNullOrEmpty()) {
                AutoSizeText(
                    text = supportingText,
                    maxLines = 3,
                    minFontSize = 8.sp,
                )
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        colors = colors,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AutoSizeText(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )

                headlineFlag()
            }
        },
    )
}

@Composable
fun BooleanListItem(
    headlineText: String,
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    shape: Shape = Shapes.StandaloneShape,
    headlineFlag: @Composable RowScope.() -> Unit = {},
) {
    ListItem(
        headlineText = headlineText,
        trailingContent = {
            Switch(
                checked = currentValue,
                onCheckedChange = onValueChange,
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
        supportingText = supportingText,
        shape = shape,
        headlineFlag = headlineFlag,
    )
}

@Composable
fun PickerListItem(
    headlineText: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    shape: Shape = Shapes.StandaloneShape,
) {
    ListItem(
        headlineText = headlineText,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        supportingText = currentValue,
        trailingContent = trailingContent,
        shape = shape,
    )
}
