package com.fibelatti.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.foundation.Shapes

@Composable
fun <T> SelectionDialogBottomSheet(
    sheetState: AppSheetState,
    options: List<T>,
    optionName: (T) -> String,
    onOptionSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionKey: ((T) -> Any)? = null,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    mode: SelectionDialogBottomSheetMode = SelectionDialogBottomSheetMode.Buttons,
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SelectionDialogContent(
            options = options,
            optionKey = optionKey,
            optionName = optionName,
            onOptionSelect = { option ->
                sheetState.hideBottomSheet(onHidden = { onOptionSelect(option) })
            },
            header = header,
            footer = footer,
            mode = mode,
        )
    }
}

@Composable
fun <T : Any> SelectionDialogBottomSheet(
    sheetState: AppSheetState,
    title: String,
    options: List<T>,
    optionName: (T) -> String,
    onOptionSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionKey: ((T) -> Any)? = null,
    footer: @Composable () -> Unit = {},
    mode: SelectionDialogBottomSheetMode = SelectionDialogBottomSheetMode.Buttons,
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SelectionDialogContent(
            options = options,
            optionKey = optionKey,
            optionName = optionName,
            onOptionSelect = { option ->
                sheetState.hideBottomSheet(onHidden = { onOptionSelect(option) })
            },
            header = {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            footer = footer,
            mode = mode,
        )
    }
}

sealed class SelectionDialogBottomSheetMode {
    data object Buttons : SelectionDialogBottomSheetMode()
    data class Radio<T>(val currentSelection: T) : SelectionDialogBottomSheetMode()
}

@Composable
private fun <T> SelectionDialogContent(
    options: List<T>,
    optionKey: ((T) -> Any)?,
    optionName: (T) -> String,
    onOptionSelect: (T) -> Unit,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    mode: SelectionDialogBottomSheetMode,
) {
    when (mode) {
        is SelectionDialogBottomSheetMode.Buttons -> {
            SelectionDialogLazyListContent(
                options = options,
                optionKey = optionKey,
                optionName = optionName,
                onOptionSelect = onOptionSelect,
                header = header,
                footer = footer,
            )
        }

        is SelectionDialogBottomSheetMode.Radio<*> -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                header()

                RadioGroup(
                    items = options,
                    itemSelected = { option -> option == mode.currentSelection },
                    onItemClick = onOptionSelect,
                    itemTitle = optionName,
                    modifier = Modifier.fillMaxWidth(),
                )

                footer()
            }
        }
    }
}

@Composable
private fun <T> SelectionDialogLazyListContent(
    options: List<T>,
    optionKey: ((T) -> Any)?,
    optionName: (T) -> String,
    onOptionSelect: (T) -> Unit,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val keyProvider: ((Int, T) -> Any)? = if (optionKey != null) {
        { _: Int, option: T -> optionKey(option) }
    } else {
        null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        stickyHeader {
            Column {
                header()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        itemsIndexed(items = options, key = keyProvider) { index: Int, option: T ->
            val shape: Shape = when {
                options.size == 1 -> Shapes.StandaloneShape
                index == 0 -> Shapes.TopShape
                index == options.lastIndex -> Shapes.BottomShape
                else -> Shapes.MiddleShape
            }

            ListItem(
                headlineText = optionName(option),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable(onClick = { onOptionSelect(option) }, role = Role.Button),
                shape = shape,
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }

        item {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                footer()
            }
        }
    }
}
