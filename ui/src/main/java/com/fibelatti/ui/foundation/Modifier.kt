@file:Suppress("unused")

package com.fibelatti.ui.foundation

import androidx.compose.ui.Modifier

inline fun Modifier.conditional(
    predicate: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier = { this },
): Modifier = if (predicate) then(ifTrue(Modifier)) else then(ifFalse(Modifier))
