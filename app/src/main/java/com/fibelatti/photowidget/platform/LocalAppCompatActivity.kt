package com.fibelatti.photowidget.platform

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext

/**
 * Provides the [AppCompatActivity] belonging to the current [LocalContext].
 *
 * Alternative API to `LocalActivity` that returns an [AppCompatActivity] instead.
 */
val LocalAppCompatActivity: ProvidableCompositionLocal<AppCompatActivity> = compositionLocalWithComputedDefaultOf {
    LocalContext.currentValue.findOwner() ?: error("No AppCompatActivity found in the Context hierarchy")
}
