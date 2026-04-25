package com.fibelatti.photowidget.platform

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Pop the last entry from the receiver [NavBackStack] if its [size][NavBackStack.size] is greater than 1.
 *
 * @return the removed [NavKey] if pop did happen, null otherwise.
 */
fun <T : NavKey> NavBackStack<T>.popNavKey(): T? {
    return if (size > 1) removeLastOrNull() else null
}
