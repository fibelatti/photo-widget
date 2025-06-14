package com.fibelatti.ui.foundation

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.fibelatti.ui.text.AutoSizeText

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ConnectedButtonRowItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    itemIndex: Int,
    itemCount: Int,
    label: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
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
    ) {
        AutoSizeText(
            text = label,
            style = textStyle,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}
