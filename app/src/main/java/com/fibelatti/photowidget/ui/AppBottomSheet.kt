@file:OptIn(ExperimentalMaterial3Api::class)

package com.fibelatti.photowidget.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Custom [ModalBottomSheet] component, which abstracts the show/hide orchestration.
 *
 * Sample usage:
 * ```kotlin
 * val sheetState = rememberAppSheetState()
 *
 * Button(
 *   onClick = sheetState::showBottomSheet
 * ) {
 *   Text("Show bottom sheet")
 * }
 *
 * AppBottomSheet(
 *   sheetState = sheetState,
 * ) {
 *   // Custom content...
 * }
 * ```
 *
 * @param sheetState [AppSheetState] obtained with [rememberAppSheetState].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet animates to Hidden.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
fun AppBottomSheet(
    sheetState: AppSheetState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    require(sheetState is AppSheetStateImpl)

    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            // The caller requested the sheet to be visible: start the show animation
            // The sheet will be added to the composition below
            sheetState.state.show()
        }
    }

    // The sheet is neither visible or requested to be visible: remove it from the composition
    if (!sheetState.isBottomSheetShowing) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = {
            // The sheet was dismissed with a gesture or click outside: clean up the custom state and notify the caller
            sheetState.isVisible = false
            onDismissRequest()
        },
        modifier = modifier,
        sheetState = sheetState.state,
        content = content,
    )
}

/**
 * Creates and remembers an [AppSheetState] instance, used to control the visibility of an [AppBottomSheet].
 */
@Composable
fun rememberAppSheetState(
    skipPartiallyExpanded: Boolean = true,
): AppSheetState {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val scope: CoroutineScope = rememberCoroutineScope()

    return remember(skipPartiallyExpanded) { AppSheetStateImpl(sheetState, scope) }
}

/**
 * Marker interface used to control the behavior of an [AppBottomSheet].
 *
 * @see rememberAppSheetState
 */
interface AppSheetState

private data class AppSheetStateImpl(
    val state: SheetState,
    val scope: CoroutineScope,
) : AppSheetState {

    var isVisible: Boolean by mutableStateOf(false)

    var data: Any? by mutableStateOf(null)
}

/**
 * Indicates whether the bottom sheet is currently visible, or requested to be visible.
 */
private val AppSheetState.isBottomSheetShowing: Boolean
    get() {
        require(this is AppSheetStateImpl)
        // `Or` is important here to add the `ModalBottomSheet` to the composition before it begins animating
        // otherwise it would simply appear the first time. It works as expected if opened again.
        return state.isVisible || isVisible
    }

/**
 * Shows the bottom sheet associated with the received [AppSheetState].
 *
 * @param data optional data to be passed to the bottom sheet. See [AppSheetState.data].
 */
fun AppSheetState.showBottomSheet(data: Any? = null) {
    require(this is AppSheetStateImpl)
    // Simply mark it to be displayed and let the state change do its thing in `AppBottomSheet`
    this.isVisible = true
    this.data = data
}

/**
 * Hides the bottom sheet associated with the received [AppSheetState].
 */
fun AppSheetState.hideBottomSheet() {
    require(this is AppSheetStateImpl)
    scope.launch { state.hide() }
        .invokeOnCompletion {
            // Clean up the state on completion to remove the sheet from the composition
            if (!state.isVisible) {
                isVisible = false
            }
        }
}

/**
 * Retrieves the data associated with the bottom sheet, if any was provided when calling [showBottomSheet].
 *
 * This **always returns null** before [showBottomSheet] is called, so take this into account when relying on this value
 * for your composition. It's likely that whenever the data is missing, downstream items should not be in the
 * composition yet.
 */
@Suppress("UNCHECKED_CAST")
fun <T> AppSheetState.data(): T? {
    require(this is AppSheetStateImpl)
    return data as? T
}
