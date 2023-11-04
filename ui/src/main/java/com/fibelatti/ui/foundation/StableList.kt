@file:Suppress("Unused")

package com.fibelatti.ui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * An [Immutable] class that wraps a [List] of [T] in order to provide stability to a composable
 * when needed. The given [value] is assumed to be immutable.
 */
@Stable
data class StableList<T>(val value: List<T> = emptyList())

fun <T> stableListOf(vararg items: T): StableList<T> = StableList(items.toList())

fun <T> List<T>.toStableList(): StableList<T> = StableList(value = toList())

fun <T> Array<T>.toStableList(): StableList<T> = toList().toStableList()
