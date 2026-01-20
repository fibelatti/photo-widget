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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R

@Composable
fun WarningSign(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    showDismissButton: Boolean = false,
    dismissButtonText: String = stringResource(R.string.photo_widget_action_got_it),
    onDismissClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFFFFE57F),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = Color.Black,
            )

            Text(
                text = text,
                color = Color.Black,
                style = textStyle,
            )
        }

        if (showDismissButton) {
            TextButton(
                onClick = onDismissClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = dismissButtonText, color = Color.Black)
            }
        }
    }
}
