package com.fibelatti.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
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
    optionIcon: (T) -> Int? = { null },
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SelectionDialogContent(
            options = options,
            optionKey = optionKey,
            optionName = optionName,
            optionIcon = optionIcon,
            onOptionSelect = { option ->
                sheetState.hideBottomSheet(onHidden = { onOptionSelect(option) })
            },
            header = header,
            footer = footer,
        )
    }
}

@Composable
fun <T> SelectionDialogBottomSheet(
    sheetState: AppSheetState,
    title: String,
    options: List<T>,
    optionName: (T) -> String,
    onOptionSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionKey: ((T) -> Any)? = null,
    optionIcon: (T) -> Int? = { null },
    footer: @Composable () -> Unit = {},
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SelectionDialogContent(
            options = options,
            optionKey = optionKey,
            optionName = optionName,
            optionIcon = optionIcon,
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
        )
    }
}

@Composable
private fun <T> SelectionDialogContent(
    options: List<T>,
    optionKey: ((T) -> Any)?,
    optionName: (T) -> String,
    optionIcon: (T) -> Int?,
    onOptionSelect: (T) -> Unit,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    val keyProvider: ((Int, T) -> Any)? by rememberUpdatedState(
        if (optionKey != null) {
            { _: Int, option: T -> optionKey(option) }
        } else {
            null
        },
    )

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
                options.size == 1 -> Shapes.MiddleShape
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
                trailingContent = {
                    optionIcon(option)?.let {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = "",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
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
