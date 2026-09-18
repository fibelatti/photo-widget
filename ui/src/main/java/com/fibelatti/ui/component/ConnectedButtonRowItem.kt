package com.fibelatti.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ConnectedButtonRowItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    itemIndex: Int,
    itemCount: Int,
    label: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    contentPadding: PaddingValues = ConnectedButtonRowDefaults.ContentPadding,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.RadioButton },
        shapes = when (itemIndex) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            itemCount - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        contentPadding = contentPadding,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = textStyle,
        )
    }
}

object ConnectedButtonRowDefaults {

    private val HorizontalPadding: Dp = 4.dp

    /**
     * Content padding for the items of a connected button row.
     *
     * Items of such a row are laid out with weights, so each one is measured against a hard maximum
     * width. The Material default reserves 16dp on either side, which on a compact screen can leave
     * a segment narrower than the word it holds and wrap a single-word label onto a second line.
     */
    val ContentPadding: PaddingValues
        get() = ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight).let { default ->
            PaddingValues(
                start = HorizontalPadding,
                top = default.calculateTopPadding(),
                end = HorizontalPadding,
                bottom = default.calculateBottomPadding(),
            )
        }
}
