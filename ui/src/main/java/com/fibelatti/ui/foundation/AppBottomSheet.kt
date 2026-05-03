@file:OptIn(ExperimentalMaterial3Api::class)

package com.fibelatti.ui.foundation

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    sheetState.checkTypeRequirement()

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

    CompositionLocalProvider(
        LocalOverscrollFactory provides null,
    ) {
        ModalBottomSheet(
            onDismissRequest = {
                // The sheet was dismissed with a gesture or click outside;
                // Clean up the custom state and notify the caller
                sheetState.isVisible = false
                onDismissRequest()
            },
            modifier = modifier,
            sheetState = sheetState.state,
            content = content,
        )
    }
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
@Stable
sealed interface AppSheetState

private data class AppSheetStateImpl(
    val state: SheetState,
    val scope: CoroutineScope,
) : AppSheetState {

    var isVisible: Boolean by mutableStateOf(false)

    var data: Any? by mutableStateOf(null)

    var hideJob: Job? = null
}

/**
 * Indicates whether the bottom sheet is currently visible, or requested to be visible.
 */
private val AppSheetState.isBottomSheetShowing: Boolean
    get() {
        checkTypeRequirement()
        // `Or` is important here to add the `ModalBottomSheet` to the composition before it begins animating
        // otherwise it would simply appear the first time. It works as expected if opened again.
        return state.isVisible || isVisible
    }

/**
 * Convenience function to check the validity of the receiver [AppSheetState] before performing any operations on it.
 */
@OptIn(ExperimentalContracts::class)
private fun AppSheetState.checkTypeRequirement() {
    contract { returns() implies (this@checkTypeRequirement is AppSheetStateImpl) }
    require(this is AppSheetStateImpl) { "AppBottomSheet must be used with rememberAppSheetState." }
}

/**
 * Shows the bottom sheet associated with the received [AppSheetState].
 *
 * @param data optional data to be passed to the bottom sheet. See [AppSheetState.data].
 */
fun AppSheetState.showBottomSheet(data: Any? = null) {
    checkTypeRequirement()
    scope.launch {
        // Finish the previously requested hide job if one exists to deliver the callbacks before showing again
        this@showBottomSheet.hideJob?.join()
        this@showBottomSheet.hideJob = null
        // Simply mark it to be displayed and let the state change do its thing in `AppBottomSheet`
        this@showBottomSheet.isVisible = true
        this@showBottomSheet.data = data
    }
}

/**
 * Hides the bottom sheet associated with the received [AppSheetState].
 *
 * If a hide is already in flight (e.g. multiple call sites each request a hide on selection), the existing animation
 * is reused — [onHidden] is chained onto its completion instead of starting a new one. This avoids racing
 * `state.hide()` calls that can leave the underlying [SheetState] saved in an expanded state across configuration
 * changes.
 *
 * @param onHidden optional callback invoked after the hide animation completes. Use this when the caller needs to
 * trigger work that would otherwise interrupt the animation (e.g. a configuration change), to avoid leaving the
 * underlying [SheetState] saved as expanded.
 */
fun AppSheetState.hideBottomSheet(onHidden: () -> Unit = {}) {
    checkTypeRequirement()

    val inFlight = hideJob
    if (inFlight != null && inFlight.isActive) {
        // A hide is already animating; just chain the completion callback onto it.
        inFlight.invokeOnCompletion { cause -> if (cause == null) onHidden() }
        return
    }

    val job = scope.launch { state.hide() }
    hideJob = job
    job.invokeOnCompletion { cause ->
        if (cause != null) return@invokeOnCompletion
        // Clean up the state on completion to remove the sheet from the composition
        if (!state.isVisible) {
            isVisible = false
        }
        onHidden()
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
    checkTypeRequirement()
    return data as? T
}
