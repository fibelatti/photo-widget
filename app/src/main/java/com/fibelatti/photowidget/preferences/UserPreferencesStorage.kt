package com.fibelatti.photowidget.preferences

import android.content.Context
import androidx.core.content.edit
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.minutesToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.secondsToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetShapeBuilder
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class UserPreferencesStorage @Inject constructor(@ApplicationContext context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val _userPreferences = MutableStateFlow(
        UserPreferences(
            appearance = appearance,
            dynamicColors = dynamicColors,
            defaultSource = defaultSource,
            defaultShuffle = defaultShuffle,
            defaultIntervalEnabled = defaultIntervalEnabled,
            defaultInterval = defaultInterval,
            defaultShape = defaultShape,
            defaultCornerRadius = defaultCornerRadius,
            defaultTapAction = defaultTapAction,
            defaultIncreaseBrightness = defaultIncreaseBrightness,
        ),
    )
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    // region App
    var appearance: Appearance
        get() {
            val name = sharedPreferences.getString(Preference.APP_APPEARANCE.value, null)
            return enumValueOfOrNull<Appearance>(name) ?: Appearance.FOLLOW_SYSTEM
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.APP_APPEARANCE.value, value.name) }
            _userPreferences.update { current -> current.copy(appearance = value) }
        }

    var dynamicColors: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.APP_DYNAMIC_COLORS.value, true)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.APP_DYNAMIC_COLORS.value, value) }
            _userPreferences.update { current -> current.copy(dynamicColors = value) }
        }
    // endregion App

    // region Defaults
    var defaultSource: PhotoWidgetSource
        get() {
            val name = sharedPreferences.getString(Preference.DEFAULT_SOURCE.value, null)
            return enumValueOfOrNull<PhotoWidgetSource>(name) ?: PhotoWidgetSource.PHOTOS
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.DEFAULT_SOURCE.value, value.name) }
            _userPreferences.update { current -> current.copy(defaultSource = value) }
        }

    var defaultShuffle: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.DEFAULT_SHUFFLE.value, false)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.DEFAULT_SHUFFLE.value, value) }
            _userPreferences.update { current -> current.copy(defaultShuffle = value) }
        }

    var defaultIntervalEnabled: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.DEFAULT_INTERVAL_ENABLED.value, true)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.DEFAULT_INTERVAL_ENABLED.value, value) }
            _userPreferences.update { current -> current.copy(defaultIntervalEnabled = value) }
        }

    var defaultInterval: PhotoWidgetLoopingInterval
        get() {
            val legacyValue = sharedPreferences.getLong(Preference.LEGACY_DEFAULT_INTERVAL.value, 0)
            val value = sharedPreferences.getLong(Preference.DEFAULT_INTERVAL.value, 0)

            return when {
                legacyValue > 0 -> legacyValue.minutesToLoopingInterval()
                value > 0 -> value.secondsToLoopingInterval()
                else -> PhotoWidgetLoopingInterval.ONE_DAY
            }
        }
        set(value) {
            sharedPreferences.edit {
                remove(Preference.LEGACY_DEFAULT_INTERVAL.value)
                putLong(Preference.DEFAULT_INTERVAL.value, value.toSeconds())
            }
            _userPreferences.update { current -> current.copy(defaultInterval = value) }
        }

    var defaultShape: String
        get() {
            return sharedPreferences.getString(Preference.DEFAULT_SHAPE.value, null)
                ?: PhotoWidgetShapeBuilder.DEFAULT_SHAPE_ID
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.DEFAULT_SHAPE.value, value) }
            _userPreferences.update { current -> current.copy(defaultShape = value) }
        }

    var defaultCornerRadius: Float
        get() {
            return sharedPreferences.getFloat(
                Preference.DEFAULT_CORNER_RADIUS.value,
                PhotoWidgetAspectRatio.DEFAULT_CORNER_RADIUS,
            )
        }
        set(value) {
            sharedPreferences.edit { putFloat(Preference.DEFAULT_CORNER_RADIUS.value, value) }
            _userPreferences.update { current -> current.copy(defaultCornerRadius = value) }
        }

    var defaultTapAction: PhotoWidgetTapAction
        get() {
            val name = sharedPreferences.getString(Preference.DEFAULT_TAP_ACTION.value, null)
            return enumValueOfOrNull<PhotoWidgetTapAction>(name) ?: PhotoWidgetTapAction.NONE
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.DEFAULT_TAP_ACTION.value, value.name) }
            _userPreferences.update { current -> current.copy(defaultTapAction = value) }
        }

    var defaultIncreaseBrightness: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.DEFAULT_INCREASE_BRIGHTNESS.value, false)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.DEFAULT_INCREASE_BRIGHTNESS.value, value) }
            _userPreferences.update { current -> current.copy(defaultIncreaseBrightness = value) }
        }
    // endregion Defaults

    fun clearDefaults() {
        sharedPreferences.edit {
            Preference.entries
                .minus(listOf(Preference.APP_APPEARANCE, Preference.APP_DYNAMIC_COLORS))
                .forEach { preference -> remove(preference.value) }
        }
        _userPreferences.update {
            UserPreferences(
                appearance = appearance,
                dynamicColors = dynamicColors,
                defaultSource = defaultSource,
                defaultShuffle = defaultShuffle,
                defaultIntervalEnabled = defaultIntervalEnabled,
                defaultInterval = defaultInterval,
                defaultShape = defaultShape,
                defaultCornerRadius = defaultCornerRadius,
                defaultTapAction = defaultTapAction,
                defaultIncreaseBrightness = defaultIncreaseBrightness,
            )
        }
    }

    private enum class Preference(val value: String) {
        APP_APPEARANCE(value = "user_preferences_appearance"),
        APP_DYNAMIC_COLORS(value = "user_preferences_dynamic_colors"),

        DEFAULT_ASPECT_RATIO(value = "default_aspect_ratio"),
        DEFAULT_SOURCE(value = "default_source"),
        DEFAULT_SHUFFLE(value = "default_shuffle"),
        DEFAULT_INTERVAL_ENABLED(value = "default_interval_enabled"),

        /**
         * Key from when the interval was persisted in minutes.
         */
        LEGACY_DEFAULT_INTERVAL("default_interval_minutes"),

        /**
         * Key from when the interval was migrated from minutes to seconds.
         */
        DEFAULT_INTERVAL("default_interval_seconds"),
        DEFAULT_SHAPE(value = "default_shape"),
        DEFAULT_CORNER_RADIUS(value = "default_corner_radius"),
        DEFAULT_TAP_ACTION(value = "default_tap_action"),
        DEFAULT_INCREASE_BRIGHTNESS(value = "default_increase_brightness"),
        ;

        override fun toString(): String = value
    }

    private companion object {
        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.UserPreferences"
    }
}
