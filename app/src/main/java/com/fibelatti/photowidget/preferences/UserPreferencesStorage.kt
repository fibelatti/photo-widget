package com.fibelatti.photowidget.preferences

import android.content.Context
import androidx.core.content.edit
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.minutesToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.secondsToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
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

    private val density = context.resources.displayMetrics.density

    private val _userPreferences = MutableStateFlow(
        UserPreferences(
            dataSaver = dataSaver,
            appearance = appearance,
            useTrueBlack = useTrueBlack,
            dynamicColors = dynamicColors,
            defaultSource = defaultSource,
            defaultShuffle = defaultShuffle,
            defaultDirectorySorting = defaultDirectorySorting,
            defaultCycleMode = defaultCycleMode,
            defaultShape = defaultShape,
            defaultCornerRadius = defaultCornerRadius,
            defaultOpacity = defaultOpacity,
            defaultSaturation = defaultSaturation,
            defaultBrightness = defaultBrightness,
        ),
    )
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    // region App
    var dataSaver: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.DATA_SAVER.value, true)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.DATA_SAVER.value, value) }
            _userPreferences.update { current -> current.copy(dataSaver = value) }
        }

    var appearance: Appearance
        get() {
            val name = sharedPreferences.getString(Preference.APP_APPEARANCE.value, null)
            return enumValueOfOrNull<Appearance>(name) ?: Appearance.FOLLOW_SYSTEM
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.APP_APPEARANCE.value, value.name) }
            _userPreferences.update { current -> current.copy(appearance = value) }
        }

    var useTrueBlack: Boolean
        get() {
            return sharedPreferences.getBoolean(Preference.USE_TRUE_BLACK.value, false)
        }
        set(value) {
            sharedPreferences.edit { putBoolean(Preference.USE_TRUE_BLACK.value, value) }
            _userPreferences.update { current -> current.copy(useTrueBlack = value) }
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

    var defaultDirectorySorting: DirectorySorting
        get() {
            val name = sharedPreferences.getString(Preference.DEFAULT_DIR_SORTING.value, null)
            return enumValueOfOrNull<DirectorySorting>(name) ?: DirectorySorting.NEWEST_FIRST
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.DEFAULT_DIR_SORTING.value, value.name) }
            _userPreferences.update { current -> current.copy(defaultDirectorySorting = value) }
        }

    var defaultCycleMode: PhotoWidgetCycleMode
        get() {
            val containsInterval = sharedPreferences.contains(Preference.LEGACY_DEFAULT_INTERVAL.value) ||
                sharedPreferences.contains(Preference.DEFAULT_INTERVAL.value)

            return when {
                sharedPreferences.getBoolean(Preference.DEFAULT_INTERVAL_ENABLED.value, false) -> {
                    PhotoWidgetCycleMode.Disabled
                }

                containsInterval -> {
                    val legacyValue = sharedPreferences.getLong(Preference.LEGACY_DEFAULT_INTERVAL.value, 0)
                    val value = sharedPreferences.getLong(Preference.DEFAULT_INTERVAL.value, 0)

                    val loopingInterval = when {
                        legacyValue > 0 -> legacyValue.minutesToLoopingInterval()
                        value > 0 -> value.secondsToLoopingInterval()
                        else -> PhotoWidgetLoopingInterval.ONE_DAY
                    }

                    PhotoWidgetCycleMode.Interval(loopingInterval = loopingInterval)
                }

                sharedPreferences.contains(Preference.DEFAULT_SCHEDULE.value) -> {
                    PhotoWidgetCycleMode.Schedule(
                        triggers = sharedPreferences.getStringSet(Preference.DEFAULT_SCHEDULE.value, null)
                            .orEmpty()
                            .map(Time::fromString)
                            .toSet(),
                    )
                }

                else -> PhotoWidgetCycleMode.DEFAULT
            }
        }
        set(value) {
            sharedPreferences.edit {
                remove(Preference.LEGACY_DEFAULT_INTERVAL.value)

                when (value) {
                    is PhotoWidgetCycleMode.Interval -> {
                        remove(Preference.DEFAULT_SCHEDULE.value)
                        remove(Preference.DEFAULT_INTERVAL_ENABLED.value)

                        putLong(Preference.DEFAULT_INTERVAL.value, value.loopingInterval.toSeconds())
                    }

                    is PhotoWidgetCycleMode.Schedule -> {
                        remove(Preference.DEFAULT_INTERVAL.value)
                        remove(Preference.DEFAULT_INTERVAL_ENABLED.value)

                        putStringSet(
                            Preference.DEFAULT_SCHEDULE.value,
                            value.triggers.map { (hour, minute) -> "$hour:$minute" }.toSet(),
                        )
                    }

                    is PhotoWidgetCycleMode.Disabled -> {
                        remove(Preference.DEFAULT_INTERVAL.value)
                        remove(Preference.DEFAULT_SCHEDULE.value)

                        putBoolean(Preference.DEFAULT_INTERVAL_ENABLED.value, true)
                    }
                }
            }

            _userPreferences.update { current -> current.copy(defaultCycleMode = value) }
        }

    var defaultShape: String
        get() {
            return sharedPreferences.getString(Preference.DEFAULT_SHAPE.value, null)
                ?: PhotoWidget.DEFAULT_SHAPE_ID
        }
        set(value) {
            sharedPreferences.edit { putString(Preference.DEFAULT_SHAPE.value, value) }
            _userPreferences.update { current -> current.copy(defaultShape = value) }
        }

    var defaultCornerRadius: Int
        get() {
            val legacyValue = sharedPreferences.getFloat(Preference.LEGACY_DEFAULT_CORNER_RADIUS.value, -1f)

            return if (legacyValue != -1f) {
                (legacyValue / density).roundToInt()
            } else {
                sharedPreferences.getInt(Preference.DEFAULT_CORNER_RADIUS.value, PhotoWidget.DEFAULT_CORNER_RADIUS)
            }
        }
        set(value) {
            sharedPreferences.edit {
                remove(Preference.LEGACY_DEFAULT_CORNER_RADIUS.value)
                putInt(Preference.DEFAULT_CORNER_RADIUS.value, value)
            }
            _userPreferences.update { current -> current.copy(defaultCornerRadius = value) }
        }

    var defaultOpacity: Float
        get() {
            return sharedPreferences.getFloat(
                Preference.DEFAULT_OPACITY.value,
                PhotoWidget.DEFAULT_OPACITY,
            )
        }
        set(value) {
            sharedPreferences.edit { putFloat(Preference.DEFAULT_OPACITY.value, value) }
            _userPreferences.update { current -> current.copy(defaultOpacity = value) }
        }

    var defaultSaturation: Float
        get() {
            val defaultBlackAndWhite = sharedPreferences.getBoolean(
                Preference.LEGACY_DEFAULT_BLACK_AND_WHITE.value,
                false,
            )

            return sharedPreferences.getFloat(
                Preference.DEFAULT_SATURATION.value,
                if (defaultBlackAndWhite) 0f else PhotoWidget.DEFAULT_SATURATION,
            )
        }
        set(value) {
            sharedPreferences.edit {
                putFloat(Preference.DEFAULT_SATURATION.value, value)
                remove(Preference.LEGACY_DEFAULT_BLACK_AND_WHITE.value)
            }
            _userPreferences.update { current -> current.copy(defaultSaturation = value) }
        }

    var defaultBrightness: Float
        get() {
            return sharedPreferences.getFloat(
                Preference.DEFAULT_BRIGHTNESS.value,
                PhotoWidget.DEFAULT_BRIGHTNESS,
            )
        }
        set(value) {
            sharedPreferences.edit { putFloat(Preference.DEFAULT_BRIGHTNESS.value, value) }
            _userPreferences.update { current -> current.copy(defaultBrightness = value) }
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
                dataSaver = dataSaver,
                appearance = appearance,
                useTrueBlack = useTrueBlack,
                dynamicColors = dynamicColors,
                defaultSource = defaultSource,
                defaultShuffle = defaultShuffle,
                defaultDirectorySorting = defaultDirectorySorting,
                defaultCycleMode = defaultCycleMode,
                defaultShape = defaultShape,
                defaultCornerRadius = defaultCornerRadius,
                defaultOpacity = defaultOpacity,
                defaultSaturation = defaultSaturation,
                defaultBrightness = defaultBrightness,
            )
        }
    }

    private enum class Preference(val value: String) {
        DATA_SAVER(value = "user_preferences_data_saver"),
        APP_APPEARANCE(value = "user_preferences_appearance"),
        USE_TRUE_BLACK(value = "user_preferences_use_true_black"),
        APP_DYNAMIC_COLORS(value = "user_preferences_dynamic_colors"),
        DEFAULT_SOURCE(value = "default_source"),
        DEFAULT_SHUFFLE(value = "default_shuffle"),
        DEFAULT_DIR_SORTING(value = "default_directory_sorting"),
        DEFAULT_INTERVAL_ENABLED(value = "default_interval_enabled"),

        /**
         * Key from when the interval was persisted in minutes.
         */
        LEGACY_DEFAULT_INTERVAL("default_interval_minutes"),

        /**
         * Key from when the interval was migrated from minutes to seconds.
         */
        DEFAULT_INTERVAL("default_interval_seconds"),
        DEFAULT_SCHEDULE("default_schedule"),
        DEFAULT_SHAPE(value = "default_shape"),

        /**
         * Key from when the corner radius was persisted in px.
         */
        LEGACY_DEFAULT_CORNER_RADIUS(value = "default_corner_radius"),
        DEFAULT_CORNER_RADIUS(value = "default_corner_radius_dp"),
        DEFAULT_OPACITY(value = "default_opacity"),
        DEFAULT_SATURATION(value = "default_saturation"),
        DEFAULT_BRIGHTNESS(value = "default_brightness"),

        /**
         * Key from when the black and white was persisted, before the saturation was introduced.
         */
        LEGACY_DEFAULT_BLACK_AND_WHITE(value = "default_black_and_white"),
        ;

        override fun toString(): String = value
    }

    private companion object {

        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.UserPreferences"
    }
}
