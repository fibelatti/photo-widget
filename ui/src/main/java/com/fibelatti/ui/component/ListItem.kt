package com.fibelatti.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibelatti.ui.foundation.Shapes

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
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = 4.dp,
    shape: Shape = ListItem.DefaultShape,
    headlineFlag: @Composable RowScope.() -> Unit = {},
) {
    androidx.compose.material3.ListItem(
        headlineContent = {
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
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItem.MinHeight)
            .clip(shape),
        supportingContent = {
            if (!supportingText.isNullOrEmpty()) {
                AutoSizeText(
                    text = supportingText,
                    maxLines = 3,
                    minFontSize = 8.sp,
                )
            }
        },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
        tonalElevation = tonalElevation,
    )
}
