package com.fibelatti.photowidget

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.fibelatti.photowidget.home.HomeActivity
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.tooling.DEFAULT_TIMEOUT
import com.fibelatti.photowidget.tooling.TestPhotosRule
import com.fibelatti.photowidget.tooling.awaitPinnedWidgetId
import com.fibelatti.photowidget.tooling.confirmCrop
import com.fibelatti.photowidget.tooling.confirmLauncherPinDialog
import com.fibelatti.photowidget.tooling.loadWidget
import com.fibelatti.photowidget.tooling.setCornerRadius
import com.fibelatti.photowidget.tooling.string
import com.fibelatti.photowidget.tooling.stubPhotoPicker
import com.fibelatti.photowidget.tooling.targetContext
import com.fibelatti.photowidget.tooling.widgetIds
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PhotoWidgetEndToEndTests {

    @get:Rule(order = 0)
    val intentsRule = IntentsRule()

    @get:Rule(order = 1)
    val testPhotos = TestPhotosRule()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<HomeActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun allHomeTabsAreDisplayed() {
        launchHomeActivity()

        with(composeRule) {
            onNodeWithText(string(R.string.photo_widget_home_new)).assertIsSelected()
            onNodeWithText(string(R.string.photo_widget_home_title)).assertIsDisplayed()

            onNodeWithText(string(R.string.photo_widget_home_current)).performClick()
            onNodeWithText(string(R.string.photo_widget_home_current)).assertIsSelected()
            onNodeWithText(string(R.string.photo_widget_home_filter_all)).assertIsDisplayed()

            onNodeWithText(string(R.string.photo_widget_home_settings)).performClick()
            onNodeWithText(string(R.string.photo_widget_home_settings)).assertIsSelected()
            onNodeWithText(string(R.string.widget_defaults_title)).assertIsDisplayed()
        }
    }

    @Test
    fun newWidgetCanBeConfiguredAndAddedToTheHomeScreen() {
        // Arrange
        launchHomeActivity()

        // Act
        val widgetId: Int = createWidget(cornerRadius = CORNER_RADIUS)

        // Assert
        val widget: PhotoWidget = loadWidget(widgetId = widgetId)

        assertThat(widget.photos).hasSize(1)
        assertThat(widget.aspectRatio).isEqualTo(PhotoWidgetAspectRatio.ROUNDED_SQUARE)
        assertThat(widget.cornerRadius).isEqualTo(CORNER_RADIUS)
    }

    @Test
    fun existingWidgetCanBeEdited() {
        // Arrange
        launchHomeActivity()

        val widgetId: Int = createWidget(cornerRadius = CORNER_RADIUS)
        val widgetTag = "my-widget-$widgetId"

        // Act
        with(composeRule) {
            // The configure screen finishes once the widget is pinned, and the home screen is only
            // found by the test after it resumes.
            waitUntilAtLeastOneExists(hasText(string(R.string.photo_widget_home_current)), DEFAULT_TIMEOUT)
            onNodeWithText(string(R.string.photo_widget_home_current)).performClick()
            waitUntil(DEFAULT_TIMEOUT) {
                runCatching { onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(widgetTag)) }.isSuccess
            }
            onNodeWithTag(widgetTag).performClick()
            onNodeWithText(string(R.string.photo_widget_home_my_widget_action_edit)).performClick()

            onNodeWithText(string(R.string.photo_widget_configure_tab_appearance)).performClick()
            waitUntilAtLeastOneExists(hasText("$CORNER_RADIUS"), DEFAULT_TIMEOUT)

            setCornerRadius(value = EDITED_CORNER_RADIUS)

            onNodeWithText(string(R.string.photo_widget_configure_save_changes)).performClick()
            waitUntilDoesNotExist(hasText(string(R.string.photo_widget_configure_save_changes)), DEFAULT_TIMEOUT)
        }

        // Assert
        val widget: PhotoWidget = loadWidget(widgetId = widgetId)

        assertThat(widget.photos).hasSize(1)
        assertThat(widget.cornerRadius).isEqualTo(EDITED_CORNER_RADIUS)
    }

    @Test
    fun sharedPhotosCanBeAddedToANewWidget() {
        // Arrange
        val existingWidgetIds: List<Int> = widgetIds()
        val photos: ArrayList<Uri> = ArrayList(List(size = SHARED_PHOTO_COUNT) { testPhotos.insert() })

        launchHomeActivity(
            intent = Intent(targetContext, HomeActivity::class.java)
                .setAction(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, photos),
        )

        // Act
        onView(withText(string(R.string.photo_widget_action_got_it))).inRoot(isDialog()).perform(click())

        with(composeRule) {
            onNodeWithText(string(R.string.photo_widget_aspect_ratio_square)).performClick()

            waitUntilAtLeastOneExists(
                hasContentDescription(string(R.string.photo_widget_configure_menu_crop)),
                DEFAULT_TIMEOUT,
            )
            onNodeWithText(string(R.string.photo_widget_configure_add_to_home)).performClick()

            confirmLauncherPinDialog()
        }

        // Assert
        val widgetId: Int = composeRule.awaitPinnedWidgetId(existingWidgetIds = existingWidgetIds)
        val widget: PhotoWidget = loadWidget(widgetId = widgetId)

        assertThat(widget.photos).hasSize(SHARED_PHOTO_COUNT)
    }

    private fun launchHomeActivity(intent: Intent = Intent(targetContext, HomeActivity::class.java)) {
        scenario = ActivityScenario.launch(intent)
    }

    /**
     * Creates a widget from a single picked photo and adds it to the home screen.
     */
    private fun createWidget(cornerRadius: Int): Int {
        val existingWidgetIds: List<Int> = widgetIds()

        stubPhotoPicker(photo = testPhotos.insert())

        with(composeRule) {
            onNodeWithText(string(R.string.photo_widget_aspect_ratio_square)).performClick()

            waitUntilAtLeastOneExists(hasText(string(R.string.photo_widget_configure_pick_photo)), DEFAULT_TIMEOUT)
            onNodeWithText(string(R.string.photo_widget_configure_pick_photo)).performClick()

            confirmCrop()

            waitUntilAtLeastOneExists(hasText(string(R.string.photo_widget_configure_add_to_home)), DEFAULT_TIMEOUT)
            setCornerRadius(value = cornerRadius)

            onNodeWithText(string(R.string.photo_widget_configure_add_to_home)).performClick()

            confirmLauncherPinDialog()
        }

        return composeRule.awaitPinnedWidgetId(existingWidgetIds = existingWidgetIds)
    }

    private companion object {

        const val CORNER_RADIUS = 64
        const val EDITED_CORNER_RADIUS = 96
        const val SHARED_PHOTO_COUNT = 6
    }
}
