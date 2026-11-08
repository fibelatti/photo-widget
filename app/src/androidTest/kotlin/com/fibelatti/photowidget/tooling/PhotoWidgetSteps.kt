package com.fibelatti.photowidget.tooling

import android.app.Activity
import android.app.Instrumentation
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import com.canhub.cropper.CropImageView
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.di.PhotoWidgetEntryPoint
import com.fibelatti.photowidget.di.entryPoint
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.platform.isBackgroundRestricted
import java.util.regex.Pattern
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking

private val entryPoint: PhotoWidgetEntryPoint
    get() = entryPoint(targetContext)

fun widgetIds(): List<Int> = entryPoint.widgetProviders().photoWidgetIds()

fun loadWidget(widgetId: Int): PhotoWidget = runBlocking {
    entryPoint.loadPhotoWidgetUseCase().invoke(widgetId).last()
}

/**
 * Returns the time the interval alarm of [widgetId] is set to trigger at, or -1 if none was set.
 */
fun widgetNextCycleTime(widgetId: Int): Long {
    return entryPoint.photoWidgetStorage().getWidgetNextCycleTime(appWidgetId = widgetId)
}

fun stubPhotoPicker(vararg photos: Uri) {
    val clipData: ClipData = ClipData.newRawUri(null, photos.first()).apply {
        photos.drop(1).forEach { photo: Uri -> addItem(ClipData.Item(photo)) }
    }

    intending(hasAction(Intent.ACTION_GET_CONTENT))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().apply { this.clipData = clipData }))
}

/**
 * The crop library silently ignores a crop request until its image has loaded, which the Compose
 * test APIs can't observe because the image is shown in an interop view.
 */
fun ComposeTestRule.confirmCrop() {
    val confirmCrop: String = string(R.string.photo_widget_crop_confirm)

    waitUntilContentDescriptionExists(confirmCrop)
    waitUntil(timeoutMillis = DEFAULT_TIMEOUT, condition = ::isCropImageLoaded)
    onNodeWithContentDescription(confirmCrop).performClick()
}

private fun isCropImageLoaded(): Boolean {
    var isLoaded = false

    try {
        onView(isAssignableFrom(CropImageView::class.java)).check { view, _ ->
            isLoaded = (view as? CropImageView)?.cropRect != null
        }
    } catch (_: NoMatchingViewException) {
        return false
    }

    return isLoaded
}

fun ComposeTestRule.setCornerRadius(value: Int) {
    clickNodeWithText(R.string.photo_widget_configure_tab_appearance)
    clickNodeWithText(R.string.widget_defaults_corner_radius)

    setSliderValue(value.toFloat())

    clickNodeWithText(R.string.photo_widget_action_apply)
    waitUntilTextExists("$value")
}

fun ComposeTestRule.setIntervalInDays(days: Int) {
    clickNodeWithText(R.string.photo_widget_configure_tab_behavior)
    clickNodeWithText(R.string.widget_defaults_cycling)

    if (targetContext.isBackgroundRestricted(checkUnrestrictedBattery = true)) {
        clickNodeWithText(R.string.photo_widget_action_ignore)
    }

    waitUntilTextExists(string(R.string.photo_widget_configure_cycling_mode_interval))
    clickNodeWithText(R.string.photo_widget_configure_cycling_mode_interval)
    clickNodeWithText(R.string.photo_widget_configure_interval_days_label)

    setSliderValue(days.toFloat())

    clickNodeWithText(R.string.photo_widget_action_apply)
    waitUntilDoesNotExist(
        matcher = hasText(string(R.string.photo_widget_configure_cycling_mode_interval)),
        timeoutMillis = DEFAULT_TIMEOUT,
    )
}

/**
 * The confirmation belongs to the launcher, so it's out of reach of the Compose test APIs. Its
 * button has no resource ID and its casing varies across launchers, so the selector relies on the
 * English text and doesn't filter by clickable because the text can be in a child node.
 *
 * Compose only produces frames while the test rule is waiting, and the pin request is sent from a
 * composition side effect, so this must wait with the rule instead of [UiDevice].
 */
fun ComposeTestRule.confirmLauncherPinDialog() {
    require(AppWidgetManager.getInstance(targetContext).isRequestPinAppWidgetSupported)

    val addButton: BySelector = By.pkg(device.launcherPackageName)
        .text("add to home screen".toPattern(Pattern.CASE_INSENSITIVE))

    waitUntil(DEFAULT_TIMEOUT) { device.hasObject(addButton) }
    device.waitForIdle()
    device.findObject(addButton).click()
}

/**
 * Returns the ID of the widget placed after [existingWidgetIds] were read, once its data has been
 * saved under that ID.
 *
 * Widgets placed by earlier runs stay on the home screen until the app is uninstalled, even after
 * its data is cleared, so only the difference can be asserted. The new ID also exists before the
 * widget data is migrated to it: the pinning cache is cleared when the migration is done, and
 * loading the widget any earlier could collide with it.
 */
fun ComposeTestRule.awaitPinnedWidgetId(existingWidgetIds: List<Int>): Int {
    waitUntil(DEFAULT_TIMEOUT) { (widgetIds() - existingWidgetIds.toSet()).isNotEmpty() }
    waitUntil(DEFAULT_TIMEOUT) { entryPoint.photoWidgetPinningCache().peek() == null }

    return (widgetIds() - existingWidgetIds.toSet()).single()
}

/**
 * Opens [widgetId] for editing from the home screen, once the configure screen that pinned it has
 * finished, and waits for the configure screen to show it.
 */
fun ComposeTestRule.openWidgetForEditing(widgetId: Int) {
    val widgetTag = "my-widget-$widgetId"

    // The configure screen finishes once the widget is pinned, and the home screen is only found by
    // the test after it resumes.
    awaitScreenWith(matcher = hasText(string(R.string.photo_widget_home_current)))
    clickNodeWithText(R.string.photo_widget_home_current)
    waitUntil(DEFAULT_TIMEOUT) {
        runCatching { onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(widgetTag)) }.isSuccess
    }
    onNodeWithTag(widgetTag).performClick()
    clickNodeWithText(R.string.photo_widget_home_my_widget_action_edit)
    awaitScreenWith(matcher = hasText(string(R.string.photo_widget_configure_save_changes)))
}

fun ComposeTestRule.saveWidgetChanges() {
    clickNodeWithText(R.string.photo_widget_configure_save_changes)
    waitUntilDoesNotExist(hasText(string(R.string.photo_widget_configure_save_changes)), DEFAULT_TIMEOUT)
}
