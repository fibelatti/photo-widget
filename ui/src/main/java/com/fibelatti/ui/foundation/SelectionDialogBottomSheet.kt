package com.fibelatti.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun <T> SelectionDialogBottomSheet(
    sheetState: AppSheetState,
    title: String,
    options: List<T>,
    optionName: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionIcon: (T) -> Int? = { null },
    footer: @Composable () -> Unit = {},
) {
    AppBottomSheet(
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SelectionDialogContent(
            title = title,
            options = options,
            optionName = optionName,
            optionIcon = optionIcon,
            onOptionSelected = { option ->
                onOptionSelected(option)
                sheetState.hideBottomSheet()
            },
            footer = footer,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> SelectionDialogContent(
    title: String,
    options: List<T>,
    optionName: (T) -> String,
    optionIcon: (T) -> Int?,
    onOptionSelected: (T) -> Unit,
    footer: @Composable () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection()),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title.isNotEmpty()) {
            stickyHeader {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        items(options) { option ->
            FilledTonalButton(
                onClick = { onOptionSelected(option) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = optionName(option),
                    modifier = Modifier.weight(1F),
                    textAlign = TextAlign.Center,
                )

                optionIcon(option)?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = "",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        item {
            footer()
        }
    }
}
