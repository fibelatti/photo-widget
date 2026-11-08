package com.fibelatti.photowidget.tooling

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

const val DEFAULT_TIMEOUT: Long = 15_000L

val targetContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

fun string(@StringRes id: Int): String = targetContext.getString(id)

/**
 * Waits for a node matching [matcher] while switching between activities. Until the next activity
 * resumes there may be no Compose hierarchy at all, which the Compose test APIs report by throwing
 * instead of finding no nodes.
 */
fun ComposeTestRule.awaitScreenWith(matcher: SemanticsMatcher) {
    waitUntil(DEFAULT_TIMEOUT) {
        try {
            onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }
    }
}

fun ComposeTestRule.waitUntilTextExists(text: String) {
    waitUntilAtLeastOneExists(matcher = hasText(text), timeoutMillis = DEFAULT_TIMEOUT)
}

fun ComposeTestRule.waitUntilContentDescriptionExists(contentDescription: String) {
    waitUntilAtLeastOneExists(matcher = hasContentDescription(contentDescription), timeoutMillis = DEFAULT_TIMEOUT)
}

fun ComposeTestRule.clickNodeWithText(@StringRes id: Int) {
    onNodeWithText(string(id)).performClick()
}

fun ComposeTestRule.setSliderValue(value: Float) {
    onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(value) }
}
