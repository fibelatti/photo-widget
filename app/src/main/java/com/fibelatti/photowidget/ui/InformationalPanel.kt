package com.fibelatti.photowidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Warning

@Composable
fun InformationalPanel(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    icon: Painter = rememberVectorPainter(AppIcons.Warning),
    backgroundShape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = Color(0xFFFFE57F),
    contentColor: Color = Color.Black,
    showActionButton: Boolean = false,
    actionButtonText: String = stringResource(R.string.photo_widget_action_got_it),
    onActionButtonClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(color = backgroundColor, shape = backgroundShape)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = contentColor,
            )

            Text(
                text = text,
                color = contentColor,
                style = textStyle,
            )
        }

        if (showActionButton) {
            TextButton(
                onClick = onActionButtonClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = actionButtonText, color = contentColor)
            }
        }
    }
}
