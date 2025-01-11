package com.fibelatti.ui.foundation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.foundation.ToggleButtonGroup.SquareCorner
import com.fibelatti.ui.preview.ThemePreviews
import com.fibelatti.ui.text.AutoSizeText
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun ColumnToggleButtonGroup(
    items: List<ToggleButtonGroup.Item>,
    onButtonClick: (ToggleButtonGroup.Item) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    enabled: Boolean = true,
    shape: CornerBasedShape = MaterialTheme.shapes.large,
    colors: ToggleButtonGroup.Colors = ToggleButtonGroup.colors(),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(),
    borderWidth: Dp = ToggleButtonGroup.BorderWidth,
    border: BorderStroke = BorderStroke(borderWidth, colors.borderColor),
    buttonHeight: Dp = ToggleButtonGroup.ButtonHeight,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    iconPosition: ToggleButtonGroup.IconPosition = ToggleButtonGroup.IconPosition.Start,
) {
    Column(
        modifier = modifier.selectableGroup(),
    ) {
        val mode = remember(items) {
            when {
                items.all { it.text == "" && it.icon != EmptyPainter } -> ToggleButtonGroup.Mode.IconOnly
                else -> ToggleButtonGroup.Mode.TextAndIcon
            }
        }

        items.forEachIndexed { index, toggleButtonGroupItem ->
            ToggleButton(
                item = toggleButtonGroupItem,
                onClick = { onButtonClick(toggleButtonGroupItem) },
                mode = mode,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = buttonHeight)
                    .offset(y = borderWidth * -index),
                selected = selectedIndex == index,
                enabled = enabled,
                shape = remember(index) {
                    when (index) {
                        0 -> shape.copy(bottomStart = SquareCorner, bottomEnd = SquareCorner)
                        items.size - 1 -> shape.copy(topStart = SquareCorner, topEnd = SquareCorner)
                        else -> shape.copy(all = SquareCorner)
                    }
                },
                colors = colors,
                elevation = elevation,
                border = border,
                textStyle = textStyle,
                iconPosition = iconPosition,
            )
        }
    }
}

@Composable
@Deprecated("Prefer `SingleChoiceSegmentedButtonRow` instead.")
fun RowToggleButtonGroup(
    items: List<ToggleButtonGroup.Item>,
    onButtonClick: (ToggleButtonGroup.Item) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    enabled: Boolean = true,
    shape: CornerBasedShape = MaterialTheme.shapes.large,
    colors: ToggleButtonGroup.Colors = ToggleButtonGroup.colors(),
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(),
    borderWidth: Dp = ToggleButtonGroup.BorderWidth,
    border: BorderStroke = BorderStroke(borderWidth, colors.borderColor),
    buttonHeight: Dp = ToggleButtonGroup.ButtonHeight,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    iconPosition: ToggleButtonGroup.IconPosition = ToggleButtonGroup.IconPosition.Start,
) {
    Row(
        modifier = modifier.selectableGroup(),
    ) {
        val mode = remember(items) {
            when {
                items.all { it.text == "" && it.icon != EmptyPainter } -> ToggleButtonGroup.Mode.IconOnly
                else -> ToggleButtonGroup.Mode.TextAndIcon
            }
        }

        items.forEachIndexed { index, toggleButtonGroupItem ->
            ToggleButton(
                item = toggleButtonGroupItem,
                onClick = { onButtonClick.invoke(toggleButtonGroupItem) },
                mode = mode,
                modifier = Modifier
                    .weight(weight = 1f)
                    .defaultMinSize(minHeight = buttonHeight)
                    .offset(x = borderWidth * -index),
                selected = selectedIndex == index,
                enabled = enabled,
                shape = remember(index) {
                    when (index) {
                        0 -> shape.copy(bottomEnd = SquareCorner, topEnd = SquareCorner)
                        items.size - 1 -> shape.copy(topStart = SquareCorner, bottomStart = SquareCorner)
                        else -> shape.copy(all = SquareCorner)
                    }
                },
                colors = colors,
                elevation = elevation,
                border = border,
                textStyle = textStyle,
                iconPosition = iconPosition,
            )
        }
    }
}

@Composable
private fun ToggleButton(
    item: ToggleButtonGroup.Item,
    onClick: () -> Unit,
    mode: ToggleButtonGroup.Mode,
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    shape: CornerBasedShape,
    colors: ToggleButtonGroup.Colors,
    elevation: ButtonElevation,
    border: BorderStroke,
    textStyle: TextStyle,
    iconPosition: ToggleButtonGroup.IconPosition,
) {
    val transition = updateTransition(
        targetState = selected,
        label = "ColumnToggleButtonGroup_Transition",
    )
    val containerColor by transition.animateColor(label = "ContainerColor") { value ->
        if (value) colors.selectedButtonColor else colors.unselectedButtonColor
    }
    val textColor by transition.animateColor(label = "TextColor") { value ->
        if (value) colors.selectedTextColor else colors.unselectedTextColor
    }
    val iconColor by transition.animateColor(label = "IconColor") { value ->
        if (value) colors.selectedIconColor else colors.unselectedIconColor
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor),
        elevation = elevation,
        border = border,
        contentPadding = ToggleButtonGroup.ButtonPaddingValues,
    ) {
        ButtonContent(
            item = item,
            mode = mode,
            selected = selected,
            textColor = textColor,
            textStyle = textStyle,
            iconColor = iconColor,
            iconPosition = iconPosition,
        )
    }
}

@Composable
private fun RowScope.ButtonContent(
    item: ToggleButtonGroup.Item,
    mode: ToggleButtonGroup.Mode,
    selected: Boolean,
    textColor: Color = Color.Unspecified,
    textStyle: TextStyle = LocalTextStyle.current,
    iconColor: Color = Color.Unspecified,
    iconPosition: ToggleButtonGroup.IconPosition = ToggleButtonGroup.IconPosition.Start,
) {
    when (mode) {
        ToggleButtonGroup.Mode.TextAndIcon -> ButtonWithIconAndText(
            item = item,
            selected = selected,
            textColor = textColor,
            textStyle = textStyle,
            iconColor = iconColor,
            iconPosition = iconPosition,
        )

        ToggleButtonGroup.Mode.IconOnly -> IconContent(
            item = item,
            modifier = Modifier.align(Alignment.CenterVertically),
            color = iconColor,
        )
    }
}

@Composable
private fun RowScope.ButtonWithIconAndText(
    item: ToggleButtonGroup.Item,
    selected: Boolean,
    textColor: Color = Color.Unspecified,
    textStyle: TextStyle = LocalTextStyle.current,
    iconColor: Color = Color.Unspecified,
    iconPosition: ToggleButtonGroup.IconPosition = ToggleButtonGroup.IconPosition.Start,
) {
    when (iconPosition) {
        ToggleButtonGroup.IconPosition.Start -> {
            IconContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterVertically),
                selected = selected,
                color = iconColor,
            )
            TextContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterVertically),
                color = textColor,
                style = textStyle,
            )
        }

        ToggleButtonGroup.IconPosition.Top -> Column {
            IconContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                selected = selected,
                color = iconColor,
            )
            TextContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = textColor,
                style = textStyle,
            )
        }

        ToggleButtonGroup.IconPosition.End -> {
            TextContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterVertically),
                color = textColor,
                style = textStyle,
            )
            IconContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterVertically),
                selected = selected,
                color = iconColor,
            )
        }

        ToggleButtonGroup.IconPosition.Bottom -> Column {
            TextContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = textColor,
                style = textStyle,
            )
            IconContent(
                item = item,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                selected = selected,
                color = iconColor,
            )
        }
    }
}

@Composable
private fun IconContent(
    item: ToggleButtonGroup.Item,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color = Color.Unspecified,
) {
    if (item.icon == EmptyPainter) {
        SegmentedButtonDefaults.Icon(
            active = selected,
            activeContent = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = modifier.size(18.dp),
                    tint = color,
                )
            },
        )
    } else {
        Image(
            painter = item.icon,
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = ColorFilter.tint(color).takeUnless {
                color == Color.Transparent || color == Color.Unspecified
            },
        )
    }
}

@Composable
private fun TextContent(
    item: ToggleButtonGroup.Item,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    AutoSizeText(
        text = item.text,
        modifier = modifier.padding(horizontal = 8.dp),
        color = color,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = style,
    )
}

object ToggleButtonGroup {

    internal val BorderWidth: Dp = 1.dp
    internal val ButtonHeight: Dp = 48.dp
    internal val ButtonPaddingValues = PaddingValues()
    internal val SquareCorner = CornerSize(0.dp)

    @Composable
    fun colors(
        selectedButtonColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        unselectedButtonColor: Color = MaterialTheme.colorScheme.surface,
        selectedTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        unselectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
        selectedIconColor: Color = selectedTextColor,
        unselectedIconColor: Color = unselectedTextColor,
        borderColor: Color = selectedButtonColor,
    ): Colors = Colors(
        selectedButtonColor = selectedButtonColor,
        unselectedButtonColor = unselectedButtonColor,
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        selectedIconColor = selectedIconColor,
        unselectedIconColor = unselectedIconColor,
        borderColor = borderColor,
    )

    @Immutable
    data class Item(
        val id: String,
        val text: String,
        val icon: Painter = EmptyPainter,
    )

    @Immutable
    data class Colors(
        val selectedButtonColor: Color,
        val unselectedButtonColor: Color,
        val selectedTextColor: Color,
        val unselectedTextColor: Color,
        val selectedIconColor: Color,
        val unselectedIconColor: Color,
        val borderColor: Color,
    )

    enum class IconPosition {
        Start,
        Top,
        End,
        Bottom,
    }

    internal enum class Mode {
        TextAndIcon,
        IconOnly,
    }
}

// region Previews
@Composable
@ThemePreviews
@Suppress("DEPRECATION")
private fun RowToggleButtonGroupPreview() {
    ExtendedTheme {
        val items = List(4) {
            ToggleButtonGroup.Item(
                id = "$it",
                text = "Button $it",
            )
        }

        RowToggleButtonGroup(
            items = items,
            onButtonClick = {},
            selectedIndex = 1,
        )
    }
}

@Composable
@ThemePreviews
private fun ColumnToggleButtonGroupPreview() {
    ExtendedTheme {
        val items = List(4) {
            ToggleButtonGroup.Item(
                id = "$it",
                text = "Button $it",
            )
        }

        ColumnToggleButtonGroup(
            items = items,
            onButtonClick = {},
            selectedIndex = 1,
        )
    }
}
// endregion Previews
