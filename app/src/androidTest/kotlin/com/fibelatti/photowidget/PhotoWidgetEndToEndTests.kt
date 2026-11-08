package com.fibelatti.photowidget

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.fibelatti.photowidget.home.HomeActivity
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.tooling.DEFAULT_TIMEOUT
import com.fibelatti.photowidget.tooling.TestPhotosRule
import com.fibelatti.photowidget.tooling.awaitAlarm
import com.fibelatti.photowidget.tooling.awaitPinnedWidgetId
import com.fibelatti.photowidget.tooling.confirmCrop
import com.fibelatti.photowidget.tooling.confirmLauncherPinDialog
import com.fibelatti.photowidget.tooling.loadWidget
import com.fibelatti.photowidget.tooling.openWidgetForEditing
import com.fibelatti.photowidget.tooling.saveWidgetChanges
import com.fibelatti.photowidget.tooling.scheduledAlarms
import com.fibelatti.photowidget.tooling.setCornerRadius
import com.fibelatti.photowidget.tooling.setIntervalInDays
import com.fibelatti.photowidget.tooling.string
import com.fibelatti.photowidget.tooling.stubPhotoPicker
import com.fibelatti.photowidget.tooling.targetContext
import com.fibelatti.photowidget.tooling.widgetIds
import com.fibelatti.photowidget.tooling.widgetNextCycleTime
import com.fibelatti.photowidget.widget.TapActionPendingIntentFactory
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
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

        // Act
        with(composeRule) {
            openWidgetForEditing(widgetId = widgetId)

            onNodeWithText(string(R.string.photo_widget_configure_tab_appearance)).performClick()
            waitUntilAtLeastOneExists(hasText("$CORNER_RADIUS"), DEFAULT_TIMEOUT)

            setCornerRadius(value = EDITED_CORNER_RADIUS)

            saveWidgetChanges()
        }

        // Assert
        val widget: PhotoWidget = loadWidget(widgetId = widgetId)

        assertThat(widget.photos).hasSize(1)
        assertThat(widget.cornerRadius).isEqualTo(EDITED_CORNER_RADIUS)
    }

    @Test
    fun editingTheTimerIntervalReschedulesTheAlarm() {
        // Arrange
        launchHomeActivity()

        val widgetId: Int = createSlideshowWidget()
        val initialAlarm: Long = composeRule.awaitAlarm(widgetId = widgetId)

        // Act
        composeRule.openWidgetForEditing(widgetId = widgetId)
        composeRule.setIntervalInDays(days = EDITED_INTERVAL_DAYS)

        val saveStart: Long = System.currentTimeMillis()
        composeRule.saveWidgetChanges()
        val saveEnd: Long = System.currentTimeMillis()

        // Assert
        val editedInterval = PhotoWidgetLoopingInterval(
            repeatInterval = EDITED_INTERVAL_DAYS.toLong(),
            timeUnit = TimeUnit.DAYS,
        )
        val intervalMillis: Long = editedInterval.run { timeUnit.toMillis(repeatInterval) }
        val rescheduledAlarm: Long = widgetNextCycleTime(widgetId = widgetId)

        assertThat(loadWidget(widgetId = widgetId).cycleMode)
            .isEqualTo(PhotoWidgetCycleMode.Interval(loopingInterval = editedInterval))
        assertThat(rescheduledAlarm).isIn(Range.closed(saveStart + intervalMillis, saveEnd + intervalMillis))
        assertThat(scheduledAlarms()).contains(rescheduledAlarm)
        assertThat(scheduledAlarms()).doesNotContain(initialAlarm)
    }

    @Test
    fun viewingTheNextPhotoReschedulesTheAlarm() {
        // Arrange
        launchHomeActivity()

        val widgetId: Int = createSlideshowWidget()
        val initialAlarm: Long = composeRule.awaitAlarm(widgetId = widgetId)

        assertThat(loadWidget(widgetId = widgetId).tapActions.right).isEqualTo(PhotoWidgetTapAction.ViewNextPhoto)

        // Act
        val tapStart: Long = System.currentTimeMillis()

        // Sends the same broadcast as the right tap area, because the widget can't be told apart from
        // the ones left on the launcher by earlier runs.
        TapActionPendingIntentFactory.getChangePhotoPendingIntent(context = targetContext, appWidgetId = widgetId)
            .send()
        val rescheduledAlarm: Long = composeRule.awaitAlarm(widgetId = widgetId, previous = initialAlarm)
        val tapEnd: Long = System.currentTimeMillis()

        // Assert
        val intervalMillis: Long = PhotoWidgetLoopingInterval.ONE_DAY.run { timeUnit.toMillis(repeatInterval) }

        assertThat(rescheduledAlarm).isIn(Range.closed(tapStart + intervalMillis, tapEnd + intervalMillis))
        assertThat(scheduledAlarms()).doesNotContain(initialAlarm)
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
     * Creates a widget from [SLIDESHOW_PHOTO_COUNT] picked photos and adds it to the home screen.
     */
    private fun createSlideshowWidget(): Int {
        val existingWidgetIds: List<Int> = widgetIds()

        stubPhotoPicker(*Array(size = SLIDESHOW_PHOTO_COUNT) { testPhotos.insert() })

        with(composeRule) {
            onNodeWithText(string(R.string.photo_widget_aspect_ratio_square)).performClick()

            waitUntilAtLeastOneExists(hasText(string(R.string.photo_widget_configure_pick_photo)), DEFAULT_TIMEOUT)
            onNodeWithText(string(R.string.photo_widget_configure_pick_photo)).performClick()

            waitUntilAtLeastOneExists(
                hasContentDescription(string(R.string.photo_widget_configure_menu_crop)),
                DEFAULT_TIMEOUT,
            )
            onNodeWithText(string(R.string.photo_widget_configure_add_to_home)).performClick()

            confirmLauncherPinDialog()
        }

        val widgetId: Int = composeRule.awaitPinnedWidgetId(existingWidgetIds = existingWidgetIds)

        assertThat(loadWidget(widgetId = widgetId).photos).hasSize(SLIDESHOW_PHOTO_COUNT)

        return widgetId
    }

    /**
     * Creates a widget from a single picked photo and adds it to the home screen.
     */
    private fun createWidget(cornerRadius: Int): Int {
        val existingWidgetIds: List<Int> = widgetIds()

        stubPhotoPicker(testPhotos.insert())

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
        const val SLIDESHOW_PHOTO_COUNT = 6
        const val EDITED_INTERVAL_DAYS = 3
    }
}
