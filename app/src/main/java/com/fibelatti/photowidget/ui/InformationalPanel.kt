package com.fibelatti.photowidget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Warning
import com.fibelatti.ui.foundation.Shapes
import com.fibelatti.ui.theme.ExtendedTheme

private const val INLINE_ICON_ID = "informational-panel-icon"

@Composable
fun InformationalPanel(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    icon: Painter = rememberVectorPainter(AppIcons.Warning),
    backgroundShape: Shape = Shapes.StandaloneShape,
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
    ) {
        Text(
            text = buildAnnotatedString {
                appendInlineContent(id = INLINE_ICON_ID, alternateText = " ")
                append("  ")
                append(text)
            },
            color = contentColor,
            style = textStyle,
            inlineContent = mapOf(
                INLINE_ICON_ID to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.4.em,
                        height = 1.4.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = contentColor,
                    )
                },
            ),
        )

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

@PreviewLightDark
@Composable
private fun InformationalPanelPreview(
    @PreviewParameter(LoremIpsum::class) text: String,
) {
    ExtendedTheme {
        InformationalPanel(
            text = text.take(200),
            showActionButton = true,
        )
    }
}
