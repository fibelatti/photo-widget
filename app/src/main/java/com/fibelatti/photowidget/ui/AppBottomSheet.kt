@file:OptIn(ExperimentalMaterial3Api::class)

package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppBottomSheet(
    sheetState: AppSheetState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!sheetState.isBottomSheetShowing) return
    require(sheetState is AppSheetStateImpl)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState.state,
        content = content,
    )
}

@Composable
fun rememberAppSheetState(
    skipPartiallyExpanded: Boolean = true,
): AppSheetState {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val scope: CoroutineScope = rememberCoroutineScope()

    return remember(skipPartiallyExpanded) { AppSheetStateImpl(sheetState, scope) }
}

interface AppSheetState

private data class AppSheetStateImpl(
    val state: SheetState,
    val scope: CoroutineScope,
) : AppSheetState

val AppSheetState.isBottomSheetShowing: Boolean
    get() {
        require(this is AppSheetStateImpl)
        return state.isVisible || state.targetValue != SheetValue.Hidden
    }

fun AppSheetState.showBottomSheet() {
    require(this is AppSheetStateImpl)
    scope.launch { state.show() }
}

fun AppSheetState.hideBottomSheet() {
    require(this is AppSheetStateImpl)
    scope.launch { state.hide() }
}
