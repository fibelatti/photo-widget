@file:Suppress("Unused")

package com.fibelatti.ui.foundation

import androidx.compose.runtime.Immutable
import java.util.Collections
import java.util.Objects

/**
 * An [Immutable] wrapper class that delegates to a [List] of [T] in order to provide stability to
 * a composable when needed.
 */
@Immutable
class StableList<T> private constructor(private val items: List<T>) : List<T> by items {

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is StableList<*> -> false
        else -> items == other.items
    }

    override fun hashCode(): Int = Objects.hashCode(items)

    companion object {

        operator fun <T> invoke(
            value: List<T>,
        ): StableList<T> = StableList(items = Collections.unmodifiableList(value))
    }
}

fun <T> stableListOf(vararg items: T): StableList<T> = StableList(value = items.toList())

fun <T> List<T>.toStableList(): StableList<T> = StableList(value = this)

fun <T> Array<T>.toStableList(): StableList<T> = StableList(value = this.toList())
