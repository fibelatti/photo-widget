package com.fibelatti.photowidget.widget.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.fibelatti.photowidget.model.LegacyPhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.minutesToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval.Companion.secondsToLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhotoWidgetSharedPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val userPreferencesStorage: UserPreferencesStorage,
) {

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveWidgetSource(appWidgetId: Int, source: PhotoWidgetSource) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.SOURCE}$appWidgetId", source.name)
        }
    }

    fun getWidgetSource(appWidgetId: Int): PhotoWidgetSource {
        val name = sharedPreferences.getString("${PreferencePrefix.SOURCE}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetSource>(name) ?: userPreferencesStorage.defaultSource
    }

    fun saveWidgetSyncedDir(appWidgetId: Int, dirUri: Set<Uri>) {
        sharedPreferences.edit {
            putStringSet("${PreferencePrefix.SYNCED_DIR}$appWidgetId", dirUri.map { it.toString() }.toSet())
        }
    }

    fun getWidgetSyncDir(appWidgetId: Int): Set<Uri> {
        val legacyUriString = sharedPreferences.getString("${PreferencePrefix.LEGACY_SYNCED_DIR}$appWidgetId", null)
            ?.let(Uri::parse)

        if (legacyUriString != null) {
            saveWidgetSyncedDir(appWidgetId = appWidgetId, setOf(legacyUriString))
            sharedPreferences.edit { remove("${PreferencePrefix.LEGACY_SYNCED_DIR}$appWidgetId") }
        }

        return sharedPreferences.getStringSet("${PreferencePrefix.SYNCED_DIR}$appWidgetId", null)
            .orEmpty()
            .map(Uri::parse)
            .toSet()
    }

    fun getWidgetOrder(appWidgetId: Int): List<String>? {
        return sharedPreferences.getString("${PreferencePrefix.LEGACY_ORDER}$appWidgetId", null)
            ?.split(",")
            // Legacy data, remove it as it will be migrated to the DB
            ?.also { sharedPreferences.edit { remove("${PreferencePrefix.LEGACY_ORDER}$appWidgetId") } }
    }

    fun saveWidgetShuffle(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.SHUFFLE}$appWidgetId", value)
        }
    }

    fun getWidgetShuffle(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean(
            "${PreferencePrefix.SHUFFLE}$appWidgetId",
            userPreferencesStorage.defaultShuffle,
        )
    }

    fun saveWidgetCycleMode(appWidgetId: Int, cycleMode: PhotoWidgetCycleMode) {
        sharedPreferences.edit {
            remove("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId")
            remove("${PreferencePrefix.LEGACY_INTERVAL_MINUTES}$appWidgetId")

            when (cycleMode) {
                is PhotoWidgetCycleMode.Interval -> {
                    remove("${PreferencePrefix.SCHEDULE}$appWidgetId")
                    remove("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId")

                    putLong("${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId", cycleMode.loopingInterval.toSeconds())
                }

                is PhotoWidgetCycleMode.Schedule -> {
                    remove("${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId")
                    remove("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId")

                    putStringSet(
                        "${PreferencePrefix.SCHEDULE}$appWidgetId",
                        cycleMode.triggers.map { (hour, minute) -> "$hour:$minute" }.toSet(),
                    )
                }

                is PhotoWidgetCycleMode.Disabled -> {
                    remove("${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId")
                    remove("${PreferencePrefix.SCHEDULE}$appWidgetId")

                    putBoolean("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId", false)
                }
            }
        }
    }

    fun getWidgetCycleMode(appWidgetId: Int): PhotoWidgetCycleMode {
        val containsEnabled = sharedPreferences.contains("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId")
        val containsInterval = sharedPreferences.contains("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId") ||
            sharedPreferences.contains("${PreferencePrefix.LEGACY_INTERVAL_MINUTES}$appWidgetId") ||
            sharedPreferences.contains("${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId")

        return when {
            containsEnabled && !getWidgetIntervalEnabled(appWidgetId) -> PhotoWidgetCycleMode.Disabled

            containsInterval -> PhotoWidgetCycleMode.Interval(loopingInterval = getWidgetInterval(appWidgetId))

            sharedPreferences.contains("${PreferencePrefix.SCHEDULE}$appWidgetId") -> {
                PhotoWidgetCycleMode.Schedule(
                    triggers = sharedPreferences.getStringSet("${PreferencePrefix.SCHEDULE}$appWidgetId", null)
                        .orEmpty()
                        .map(Time::fromString)
                        .toSet(),
                )
            }

            else -> userPreferencesStorage.defaultCycleMode
        }
    }

    fun saveWidgetNextCycleTime(appWidgetId: Int, nextCycleTime: Long?) {
        sharedPreferences.edit {
            if (nextCycleTime != null) {
                putLong("${PreferencePrefix.NEXT_CYCLE_TIME}$appWidgetId", nextCycleTime)
            } else {
                remove("${PreferencePrefix.NEXT_CYCLE_TIME}$appWidgetId")
            }
        }
    }

    fun getWidgetNextCycleTime(appWidgetId: Int): Long {
        return sharedPreferences.getLong("${PreferencePrefix.NEXT_CYCLE_TIME}$appWidgetId", -1)
    }

    fun saveWidgetCyclePaused(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.CYCLE_PAUSED}$appWidgetId", value)
        }
    }

    fun getWidgetCyclePaused(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean("${PreferencePrefix.CYCLE_PAUSED}$appWidgetId", false)
    }

    private fun getWidgetInterval(appWidgetId: Int): PhotoWidgetLoopingInterval {
        val legacyName = sharedPreferences.getString("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId", null)
        val legacyValue = enumValueOfOrNull<LegacyPhotoWidgetLoopingInterval>(legacyName)
        val legacyMinutes = sharedPreferences.getLong("${PreferencePrefix.LEGACY_INTERVAL_MINUTES}$appWidgetId", 0)
        val seconds = sharedPreferences.getLong("${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId", 0)

        return when {
            legacyValue != null -> {
                PhotoWidgetLoopingInterval(
                    repeatInterval = legacyValue.repeatInterval,
                    timeUnit = legacyValue.timeUnit,
                )
            }

            legacyMinutes > 0 -> legacyMinutes.minutesToLoopingInterval()

            seconds > 0 -> seconds.secondsToLoopingInterval()

            else -> PhotoWidgetLoopingInterval.ONE_DAY
        }
    }

    private fun getWidgetIntervalEnabled(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId", false)
    }

    fun getWidgetIndex(appWidgetId: Int): Int {
        return sharedPreferences.getInt("${PreferencePrefix.LEGACY_INDEX}$appWidgetId", 0)
    }

    fun saveWidgetAspectRatio(appWidgetId: Int, aspectRatio: PhotoWidgetAspectRatio) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.RATIO}$appWidgetId", aspectRatio.name)
        }
    }

    fun getWidgetAspectRatio(appWidgetId: Int): PhotoWidgetAspectRatio {
        val name = sharedPreferences.getString("${PreferencePrefix.RATIO}$appWidgetId", null)

        return enumValueOfOrNull<PhotoWidgetAspectRatio>(name) ?: PhotoWidgetAspectRatio.SQUARE
    }

    fun saveWidgetShapeId(appWidgetId: Int, shapeId: String) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.SHAPE}$appWidgetId", shapeId)
        }
    }

    fun getWidgetShapeId(appWidgetId: Int): String {
        return sharedPreferences.getString("${PreferencePrefix.SHAPE}$appWidgetId", null)
            ?: userPreferencesStorage.defaultShape
    }

    fun saveWidgetCornerRadius(appWidgetId: Int, cornerRadius: Float) {
        sharedPreferences.edit {
            putFloat("${PreferencePrefix.CORNER_RADIUS}$appWidgetId", cornerRadius)
        }
    }

    fun getWidgetCornerRadius(appWidgetId: Int): Float {
        return sharedPreferences.getFloat(
            "${PreferencePrefix.CORNER_RADIUS}$appWidgetId",
            userPreferencesStorage.defaultCornerRadius,
        )
    }

    fun saveWidgetBorder(appWidgetId: Int, border: PhotoWidgetBorder) {
        sharedPreferences.edit {
            when (border) {
                is PhotoWidgetBorder.None -> {
                    remove("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_WIDTH}$appWidgetId")
                }

                is PhotoWidgetBorder.Color -> {
                    putString("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId", border.colorHex)
                    putInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", border.width)
                    remove("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId")
                }

                is PhotoWidgetBorder.Dynamic -> {
                    putBoolean("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId", true)
                    putInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", border.width)
                    remove("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId")
                }
            }
        }
    }

    fun getWidgetBorder(appWidgetId: Int): PhotoWidgetBorder {
        val borderDynamic = sharedPreferences.getBoolean("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId", false)
        val borderColorHex = sharedPreferences.getString("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId", null)
        val borderWidth = sharedPreferences.getInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", 0)

        return when {
            borderDynamic -> PhotoWidgetBorder.Dynamic(width = borderWidth)
            borderColorHex != null -> PhotoWidgetBorder.Color(colorHex = borderColorHex, width = borderWidth)
            else -> PhotoWidgetBorder.None
        }
    }

    fun saveWidgetOpacity(appWidgetId: Int, opacity: Float) {
        sharedPreferences.edit {
            putFloat("${PreferencePrefix.OPACITY}$appWidgetId", opacity)
        }
    }

    fun getWidgetOpacity(appWidgetId: Int): Float {
        return sharedPreferences.getFloat(
            "${PreferencePrefix.OPACITY}$appWidgetId",
            userPreferencesStorage.defaultOpacity,
        )
    }

    fun saveWidgetBlackAndWhite(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.BLACK_AND_WHITE}$appWidgetId", value)
        }
    }

    fun getWidgetBlackAndWhite(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean(
            "${PreferencePrefix.BLACK_AND_WHITE}$appWidgetId",
            userPreferencesStorage.defaultBlackAndWhite,
        )
    }

    fun saveWidgetOffset(appWidgetId: Int, horizontalOffset: Int, verticalOffset: Int) {
        sharedPreferences.edit {
            putInt("${PreferencePrefix.HORIZONTAL_OFFSET}$appWidgetId", horizontalOffset)
            putInt("${PreferencePrefix.VERTICAL_OFFSET}$appWidgetId", verticalOffset)
        }
    }

    fun getWidgetOffset(appWidgetId: Int): Pair<Int, Int> {
        return sharedPreferences.getInt(
            "${PreferencePrefix.HORIZONTAL_OFFSET}$appWidgetId",
            0,
        ) to sharedPreferences.getInt(
            "${PreferencePrefix.VERTICAL_OFFSET}$appWidgetId",
            0,
        )
    }

    fun saveWidgetPadding(appWidgetId: Int, padding: Int) {
        sharedPreferences.edit {
            putInt("${PreferencePrefix.PADDING}$appWidgetId", padding)
        }
    }

    fun getWidgetPadding(appWidgetId: Int): Int {
        return sharedPreferences.getInt("${PreferencePrefix.PADDING}$appWidgetId", 0)
    }

    fun saveWidgetTapAction(appWidgetId: Int, tapAction: PhotoWidgetTapAction) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.TAP_ACTION}$appWidgetId", tapAction.serializedName)

            when (tapAction) {
                is PhotoWidgetTapAction.ViewFullScreen -> {
                    putBoolean("${PreferencePrefix.INCREASE_BRIGHTNESS}$appWidgetId", tapAction.increaseBrightness)
                    putBoolean("${PreferencePrefix.VIEW_ORIGINAL_PHOTO}$appWidgetId", tapAction.viewOriginalPhoto)
                }

                is PhotoWidgetTapAction.ViewInGallery -> {
                    putString("${PreferencePrefix.PREFERRED_GALLERY_APP}$appWidgetId", tapAction.galleryApp)
                }

                is PhotoWidgetTapAction.AppShortcut -> {
                    putString("${PreferencePrefix.APP_SHORTCUT}$appWidgetId", tapAction.appShortcut)
                }

                is PhotoWidgetTapAction.UrlShortcut -> {
                    putString("${PreferencePrefix.URL_SHORTCUT}$appWidgetId", tapAction.url)
                }

                is PhotoWidgetTapAction.ToggleCycling -> {
                    putBoolean("${PreferencePrefix.DISABLE_TAP}$appWidgetId", tapAction.disableTap)
                }

                else -> Unit
            }
        }
    }

    fun getWidgetTapAction(appWidgetId: Int): PhotoWidgetTapAction = with(sharedPreferences) {
        val name = getString("${PreferencePrefix.TAP_ACTION}$appWidgetId", null)
            ?: return userPreferencesStorage.defaultTapAction

        return PhotoWidgetTapAction.fromSerializedName(name).let { tapAction ->
            when (tapAction) {
                is PhotoWidgetTapAction.ViewFullScreen -> tapAction.copy(
                    increaseBrightness = getBoolean("${PreferencePrefix.INCREASE_BRIGHTNESS}$appWidgetId", false),
                    viewOriginalPhoto = getBoolean("${PreferencePrefix.VIEW_ORIGINAL_PHOTO}$appWidgetId", false),
                )

                is PhotoWidgetTapAction.ViewInGallery -> tapAction.copy(
                    galleryApp = getString("${PreferencePrefix.PREFERRED_GALLERY_APP}$appWidgetId", null),
                )

                is PhotoWidgetTapAction.AppShortcut -> tapAction.copy(
                    appShortcut = getString("${PreferencePrefix.APP_SHORTCUT}$appWidgetId", null),
                )

                is PhotoWidgetTapAction.UrlShortcut -> tapAction.copy(
                    url = getString("${PreferencePrefix.URL_SHORTCUT}$appWidgetId", null),
                )

                is PhotoWidgetTapAction.ToggleCycling -> tapAction.copy(
                    disableTap = getBoolean("${PreferencePrefix.DISABLE_TAP}$appWidgetId", false),
                )

                else -> tapAction
            }
        }
    }

    fun saveWidgetDeletionTimestamp(appWidgetId: Int, timestamp: Long?) {
        sharedPreferences.edit {
            if (timestamp != null) {
                putLong("${PreferencePrefix.DELETION_TIMESTAMP}$appWidgetId", timestamp)
            } else {
                remove("${PreferencePrefix.DELETION_TIMESTAMP}$appWidgetId")
            }
        }
    }

    fun getWidgetDeletionTimestamp(appWidgetId: Int): Long {
        return sharedPreferences.getLong("${PreferencePrefix.DELETION_TIMESTAMP}$appWidgetId", -1)
    }

    fun deleteWidgetData(appWidgetId: Int) {
        sharedPreferences.edit {
            PreferencePrefix.entries.forEach { prefix -> remove("$prefix$appWidgetId") }
        }
    }

    fun getKnownWidgetIds(): List<Int> {
        return sharedPreferences.all
            .filter { (key, _) -> key.startsWith(PreferencePrefix.SOURCE.value) }
            .mapNotNull { (key, _) -> key.substringAfterLast("_").toIntOrNull()?.takeIf { it > 0 } }
            .distinct()
    }

    private enum class PreferencePrefix(val value: String) {
        SOURCE(value = "appwidget_source_"),

        /**
         * Key from when initial support for directory based widgets was introduced.
         */
        LEGACY_SYNCED_DIR(value = "appwidget_synced_dir_"),

        /**
         * Key from when support for syncing multiple directories was introduced.
         */
        SYNCED_DIR(value = "appwidget_synced_dir_set_"),

        LEGACY_ORDER(value = "appwidget_order_"),
        SHUFFLE(value = "appwidget_shuffle_"),

        /**
         * Key from when the interval was persisted as [LegacyPhotoWidgetLoopingInterval].
         */
        LEGACY_INTERVAL(value = "appwidget_interval_"),

        /**
         * Key from when the interval was migrated to [PhotoWidgetLoopingInterval].
         */
        LEGACY_INTERVAL_MINUTES(value = "appwidget_interval_minutes_"),

        /**
         * Key from when the interval was migrated from minutes to seconds.
         */
        INTERVAL_SECONDS(value = "appwidget_interval_seconds_"),
        INTERVAL_ENABLED(value = "appwidget_interval_enabled_"),
        SCHEDULE(value = "appwidget_schedule_"),
        NEXT_CYCLE_TIME(value = "appwidget_next_cycle_time_"),
        CYCLE_PAUSED(value = "appwidget_cycle_paused_"),
        LEGACY_INDEX(value = "appwidget_index_"),
        LEGACY_PAST_INDICES(value = "appwidget_past_indices_"),
        RATIO(value = "appwidget_aspect_ratio_"),
        SHAPE(value = "appwidget_shape_"),
        CORNER_RADIUS(value = "appwidget_corner_radius_"),
        BORDER_COLOR_HEX(value = "appwidget_border_color_hex_"),
        BORDER_DYNAMIC(value = "appwidget_border_dynamic_"),
        BORDER_WIDTH(value = "appwidget_border_width_"),
        OPACITY(value = "appwidget_opacity_"),
        BLACK_AND_WHITE(value = "appwidget_black_and_white_"),
        HORIZONTAL_OFFSET(value = "appwidget_horizontal_offset_"),
        VERTICAL_OFFSET(value = "appwidget_vertical_offset_"),
        PADDING(value = "appwidget_padding_"),
        TAP_ACTION(value = "appwidget_tap_action_"),
        INCREASE_BRIGHTNESS(value = "appwidget_increase_brightness_"),
        VIEW_ORIGINAL_PHOTO(value = "appwidget_view_original_photo_"),
        APP_SHORTCUT(value = "appwidget_app_shortcut_"),
        URL_SHORTCUT(value = "appwidget_url_shortcut_"),
        PREFERRED_GALLERY_APP(value = "appwidget_preferred_gallery_app_"),
        DISABLE_TAP(value = "appwidget_disable_tap_"),

        DELETION_TIMESTAMP(value = "appwidget_deletion_timestamp_"),
        ;

        override fun toString(): String = value
    }

    private companion object {

        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.PhotoWidget"
    }
}
