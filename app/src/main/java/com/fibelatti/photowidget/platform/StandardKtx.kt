package com.fibelatti.photowidget.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
fun <T : R, R> T.letIf(predicate: Boolean, block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        predicate.holdsIn(block)
    }
    return if (predicate) block(this) else this
}
