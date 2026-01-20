package com.fibelatti.photowidget.widget.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.fibelatti.photowidget.model.DirectorySorting
import com.fibelatti.photowidget.model.LegacyPhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetBorder
import com.fibelatti.photowidget.model.PhotoWidgetCycleMode
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetSource
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.PhotoWidgetText
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.model.Time
import com.fibelatti.photowidget.model.minutesToLoopingInterval
import com.fibelatti.photowidget.model.repeatIntervalAsSeconds
import com.fibelatti.photowidget.model.secondsToLoopingInterval
import com.fibelatti.photowidget.platform.enumValueOfOrNull
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt

class PhotoWidgetSharedPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val userPreferencesStorage: UserPreferencesStorage,
) {

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val density = context.resources.displayMetrics.density

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

    fun saveWidgetSorting(appWidgetId: Int, sorting: DirectorySorting) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.DIR_SORTING}$appWidgetId", sorting.name)
        }
    }

    fun getWidgetSorting(appWidgetId: Int): DirectorySorting {
        val name = sharedPreferences.getString("${PreferencePrefix.DIR_SORTING}$appWidgetId", null)

        return enumValueOfOrNull<DirectorySorting>(name) ?: userPreferencesStorage.defaultDirectorySorting
    }

    fun saveWidgetCycleMode(appWidgetId: Int, cycleMode: PhotoWidgetCycleMode) {
        sharedPreferences.edit {
            remove("${PreferencePrefix.LEGACY_INTERVAL}$appWidgetId")
            remove("${PreferencePrefix.LEGACY_INTERVAL_MINUTES}$appWidgetId")

            when (cycleMode) {
                is PhotoWidgetCycleMode.Interval -> {
                    remove("${PreferencePrefix.SCHEDULE}$appWidgetId")
                    remove("${PreferencePrefix.INTERVAL_ENABLED}$appWidgetId")

                    putLong(
                        "${PreferencePrefix.INTERVAL_SECONDS}$appWidgetId",
                        cycleMode.loopingInterval.repeatIntervalAsSeconds(),
                    )
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

    fun saveWidgetLockedInApp(appWidgetId: Int, value: Boolean) {
        sharedPreferences.edit {
            putBoolean("${PreferencePrefix.LOCKED_IN_APP}$appWidgetId", value)
        }
    }

    fun getWidgetLockedInApp(appWidgetId: Int): Boolean {
        return sharedPreferences.getBoolean("${PreferencePrefix.LOCKED_IN_APP}$appWidgetId", false)
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

        return enumValueOfOrNull<PhotoWidgetAspectRatio>(name) ?: userPreferencesStorage.defaultAspectRatio
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

    fun saveWidgetCornerRadius(appWidgetId: Int, cornerRadius: Int) {
        sharedPreferences.edit {
            remove("${PreferencePrefix.LEGACY_CORNER_RADIUS}$appWidgetId")
            putInt("${PreferencePrefix.CORNER_RADIUS_DP}$appWidgetId", cornerRadius)
        }
    }

    fun getWidgetCornerRadius(appWidgetId: Int): Int {
        val legacyValue = sharedPreferences.getFloat("${PreferencePrefix.LEGACY_CORNER_RADIUS}$appWidgetId", -1f)

        return if (legacyValue != -1f) {
            (legacyValue / density).roundToInt()
        } else {
            sharedPreferences.getInt(
                "${PreferencePrefix.CORNER_RADIUS_DP}$appWidgetId",
                userPreferencesStorage.defaultCornerRadius,
            )
        }
    }

    fun saveWidgetBorder(appWidgetId: Int, border: PhotoWidgetBorder) {
        sharedPreferences.edit {
            when (border) {
                is PhotoWidgetBorder.None -> {
                    remove("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_DYNAMIC_TYPE}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_COLOR_PALETTE_TYPE}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_WIDTH}$appWidgetId")
                }

                is PhotoWidgetBorder.Color -> {
                    putString("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId", border.colorHex)
                    putInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", border.width)
                    remove("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_DYNAMIC_TYPE}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_COLOR_PALETTE_TYPE}$appWidgetId")
                }

                is PhotoWidgetBorder.Dynamic -> {
                    putBoolean("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId", true)
                    putString("${PreferencePrefix.BORDER_DYNAMIC_TYPE}$appWidgetId", border.type.name)
                    putInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", border.width)
                    remove("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_COLOR_PALETTE_TYPE}$appWidgetId")
                }

                is PhotoWidgetBorder.MatchPhoto -> {
                    putString("${PreferencePrefix.BORDER_COLOR_PALETTE_TYPE}$appWidgetId", border.type.name)
                    putInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", border.width)
                    remove("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_DYNAMIC_TYPE}$appWidgetId")
                    remove("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId")
                }
            }
        }
    }

    fun getWidgetBorder(appWidgetId: Int): PhotoWidgetBorder {
        val borderDynamic = sharedPreferences.getBoolean("${PreferencePrefix.BORDER_DYNAMIC}$appWidgetId", false)
        val borderDynamicType = sharedPreferences.getString(
            "${PreferencePrefix.BORDER_DYNAMIC_TYPE}$appWidgetId",
            null,
        )
        val borderColorHex = sharedPreferences.getString("${PreferencePrefix.BORDER_COLOR_HEX}$appWidgetId", null)
        val borderColorPaletteType = sharedPreferences.getString(
            "${PreferencePrefix.BORDER_COLOR_PALETTE_TYPE}$appWidgetId",
            null,
        )
        val borderWidth = sharedPreferences.getInt("${PreferencePrefix.BORDER_WIDTH}$appWidgetId", 0)

        return when {
            borderDynamic -> PhotoWidgetBorder.Dynamic(
                width = borderWidth,
                type = enumValueOfOrNull<PhotoWidgetBorder.Dynamic.Type>(borderDynamicType)
                    ?: PhotoWidgetBorder.Dynamic.Type.PRIMARY_INVERSE,
            )

            borderColorPaletteType != null -> PhotoWidgetBorder.MatchPhoto(
                width = borderWidth,
                type = enumValueOfOrNull<PhotoWidgetBorder.MatchPhoto.Type>(borderColorPaletteType)
                    ?: PhotoWidgetBorder.MatchPhoto.Type.DOMINANT,
            )

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

    fun saveWidgetSaturation(appWidgetId: Int, saturation: Float) {
        sharedPreferences.edit {
            putFloat("${PreferencePrefix.SATURATION}$appWidgetId", saturation)
            remove("${PreferencePrefix.LEGACY_BLACK_AND_WHITE}$appWidgetId")
        }
    }

    fun getWidgetSaturation(appWidgetId: Int): Float {
        val blackAndWhite = sharedPreferences.getBoolean(
            "${PreferencePrefix.LEGACY_BLACK_AND_WHITE}$appWidgetId",
            false,
        )

        return sharedPreferences.getFloat(
            "${PreferencePrefix.SATURATION}$appWidgetId",
            if (blackAndWhite) 0f else userPreferencesStorage.defaultSaturation,
        )
    }

    fun saveWidgetBrightness(appWidgetId: Int, brightness: Float) {
        sharedPreferences.edit {
            putFloat("${PreferencePrefix.BRIGHTNESS}$appWidgetId", brightness)
        }
    }

    fun getWidgetBrightness(appWidgetId: Int): Float {
        return sharedPreferences.getFloat(
            "${PreferencePrefix.BRIGHTNESS}$appWidgetId",
            userPreferencesStorage.defaultBrightness,
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

    fun saveWidgetTapAction(
        appWidgetId: Int,
        tapAction: PhotoWidgetTapAction,
        tapActionArea: TapActionArea,
    ) {
        val mainPrefKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.TAP_ACTION_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.TAP_ACTION_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.TAP_ACTION_RIGHT}$appWidgetId"
        }

        val appShortcutKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.APP_SHORTCUT_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.APP_SHORTCUT_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.APP_SHORTCUT_RIGHT}$appWidgetId"
        }

        val appFolderKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.APP_FOLDER_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.APP_FOLDER_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.APP_FOLDER_RIGHT}$appWidgetId"
        }

        val urlShortcutKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.URL_SHORTCUT_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.URL_SHORTCUT_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.URL_SHORTCUT_RIGHT}$appWidgetId"
        }

        sharedPreferences.edit {
            putString(mainPrefKey, tapAction.serializedName)

            remove(appShortcutKey)
            remove(appFolderKey)
            remove(urlShortcutKey)

            when (tapAction) {
                is PhotoWidgetTapAction.ViewFullScreen -> {
                    putBoolean("${PreferencePrefix.INCREASE_BRIGHTNESS}$appWidgetId", tapAction.increaseBrightness)
                    putBoolean("${PreferencePrefix.VIEW_ORIGINAL_PHOTO}$appWidgetId", tapAction.viewOriginalPhoto)
                    putBoolean("${PreferencePrefix.NO_SHUFFLE}$appWidgetId", tapAction.noShuffle)
                    putBoolean("${PreferencePrefix.KEEP_CURRENT_PHOTO}$appWidgetId", tapAction.keepCurrentPhoto)
                }

                is PhotoWidgetTapAction.ViewInGallery -> {
                    putString("${PreferencePrefix.PREFERRED_GALLERY_APP}$appWidgetId", tapAction.galleryApp)
                }

                is PhotoWidgetTapAction.AppShortcut -> {
                    putString(appShortcutKey, tapAction.appShortcut)
                }

                is PhotoWidgetTapAction.AppFolder -> {
                    putString(appFolderKey, tapAction.shortcuts.joinToString(separator = ",").ifEmpty { null })
                }

                is PhotoWidgetTapAction.UrlShortcut -> {
                    putString(urlShortcutKey, tapAction.url)
                }

                is PhotoWidgetTapAction.ToggleCycling -> {
                    putBoolean("${PreferencePrefix.DISABLE_TAP}$appWidgetId", tapAction.disableTap)
                }

                else -> Unit
            }
        }
    }

    fun getWidgetTapAction(
        appWidgetId: Int,
        tapActionArea: TapActionArea,
    ): PhotoWidgetTapAction = with(sharedPreferences) {
        val mainPrefKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.TAP_ACTION_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.TAP_ACTION_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.TAP_ACTION_RIGHT}$appWidgetId"
        }

        val appShortcutKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.APP_SHORTCUT_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.APP_SHORTCUT_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.APP_SHORTCUT_RIGHT}$appWidgetId"
        }

        val appFolderKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.APP_FOLDER_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.APP_FOLDER_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.APP_FOLDER_RIGHT}$appWidgetId"
        }

        val urlShortcutKey = when (tapActionArea) {
            TapActionArea.LEFT -> "${PreferencePrefix.URL_SHORTCUT_LEFT}$appWidgetId"
            TapActionArea.CENTER -> "${PreferencePrefix.URL_SHORTCUT_CENTER}$appWidgetId"
            TapActionArea.RIGHT -> "${PreferencePrefix.URL_SHORTCUT_RIGHT}$appWidgetId"
        }

        val name = getString(mainPrefKey, null) ?: return when (tapActionArea) {
            TapActionArea.LEFT -> PhotoWidgetTapAction.ViewPreviousPhoto
            TapActionArea.CENTER -> PhotoWidgetTapAction.ViewFullScreen()
            TapActionArea.RIGHT -> PhotoWidgetTapAction.ViewNextPhoto
        }

        return PhotoWidgetTapAction.fromSerializedName(name).let { tapAction ->
            when (tapAction) {
                is PhotoWidgetTapAction.ViewFullScreen -> tapAction.copy(
                    increaseBrightness = getBoolean("${PreferencePrefix.INCREASE_BRIGHTNESS}$appWidgetId", false),
                    viewOriginalPhoto = getBoolean("${PreferencePrefix.VIEW_ORIGINAL_PHOTO}$appWidgetId", false),
                    noShuffle = getBoolean("${PreferencePrefix.NO_SHUFFLE}$appWidgetId", false),
                    keepCurrentPhoto = getBoolean("${PreferencePrefix.KEEP_CURRENT_PHOTO}$appWidgetId", false),
                )

                is PhotoWidgetTapAction.ViewInGallery -> tapAction.copy(
                    galleryApp = getString("${PreferencePrefix.PREFERRED_GALLERY_APP}$appWidgetId", null),
                )

                is PhotoWidgetTapAction.AppShortcut -> tapAction.copy(
                    appShortcut = getString(appShortcutKey, null),
                )

                is PhotoWidgetTapAction.AppFolder -> {
                    val shortcuts: List<String> = getString(appFolderKey, null)
                        ?.split(",")
                        ?.filter { it.isNotEmpty() }
                        .orEmpty()

                    tapAction.copy(shortcuts = shortcuts)
                }

                is PhotoWidgetTapAction.UrlShortcut -> tapAction.copy(
                    url = getString(urlShortcutKey, null),
                )

                is PhotoWidgetTapAction.ToggleCycling -> tapAction.copy(
                    disableTap = getBoolean("${PreferencePrefix.DISABLE_TAP}$appWidgetId", false),
                )

                else -> tapAction
            }
        }
    }

    fun saveWidgetText(
        appWidgetId: Int,
        text: PhotoWidgetText,
    ) {
        sharedPreferences.edit {
            putString("${PreferencePrefix.TEXT_TYPE}$appWidgetId", text.serializedName)

            if (text is PhotoWidgetText.None) {
                remove("${PreferencePrefix.TEXT_VALUE}$appWidgetId")
                remove("${PreferencePrefix.TEXT_SIZE}$appWidgetId")
                remove("${PreferencePrefix.TEXT_VERTICAL_OFFSET}$appWidgetId")
                remove("${PreferencePrefix.TEXT_HAS_SHADOW}$appWidgetId")
            } else {
                putString("${PreferencePrefix.TEXT_VALUE}$appWidgetId", text.value)
                putInt("${PreferencePrefix.TEXT_SIZE}$appWidgetId", text.size)
                putInt("${PreferencePrefix.TEXT_VERTICAL_OFFSET}$appWidgetId", text.verticalOffset)
                putBoolean("${PreferencePrefix.TEXT_HAS_SHADOW}$appWidgetId", text.hasShadow)
            }
        }
    }

    fun getWidgetText(appWidgetId: Int): PhotoWidgetText = with(sharedPreferences) {
        val name = getString("${PreferencePrefix.TEXT_TYPE}$appWidgetId", null)

        return PhotoWidgetText.fromSerializedName(name).let { photoWidgetText ->
            when (photoWidgetText) {
                is PhotoWidgetText.None -> photoWidgetText

                is PhotoWidgetText.Label -> photoWidgetText.copy(
                    value = getString("${PreferencePrefix.TEXT_VALUE}$appWidgetId", "") ?: "",
                    size = getInt("${PreferencePrefix.TEXT_SIZE}$appWidgetId", 12),
                    verticalOffset = getInt("${PreferencePrefix.TEXT_VERTICAL_OFFSET}$appWidgetId", 0),
                    hasShadow = getBoolean("${PreferencePrefix.TEXT_HAS_SHADOW}$appWidgetId", true),
                )
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
        DIR_SORTING(value = "appwidget_dir_sorting_"),

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
        LOCKED_IN_APP(value = "appwidget_locked_in_app_"),
        LEGACY_INDEX(value = "appwidget_index_"),
        LEGACY_PAST_INDICES(value = "appwidget_past_indices_"),
        RATIO(value = "appwidget_aspect_ratio_"),
        SHAPE(value = "appwidget_shape_"),

        /**
         * Key from when the corner radius was persisted in px.
         */
        LEGACY_CORNER_RADIUS(value = "appwidget_corner_radius_"),
        CORNER_RADIUS_DP(value = "appwidget_corner_radius_dp_"),
        BORDER_COLOR_HEX(value = "appwidget_border_color_hex_"),
        BORDER_DYNAMIC(value = "appwidget_border_dynamic_"),
        BORDER_DYNAMIC_TYPE(value = "appwidget_border_dynamic_type_"),
        BORDER_COLOR_PALETTE_TYPE(value = "appwidget_border_color_palette_type_"),
        BORDER_WIDTH(value = "appwidget_border_width_"),
        OPACITY(value = "appwidget_opacity_"),
        SATURATION(value = "appwidget_saturation_"),
        BRIGHTNESS(value = "appwidget_brightness_"),

        /**
         * Key from when the black and white was persisted, before the saturation was introduced.
         */
        LEGACY_BLACK_AND_WHITE(value = "appwidget_black_and_white_"),
        HORIZONTAL_OFFSET(value = "appwidget_horizontal_offset_"),
        VERTICAL_OFFSET(value = "appwidget_vertical_offset_"),
        PADDING(value = "appwidget_padding_"),
        TAP_ACTION_LEFT(value = "appwidget_tap_action_left_"),
        TAP_ACTION_CENTER(value = "appwidget_tap_action_"),
        TAP_ACTION_RIGHT(value = "appwidget_tap_action_right_"),
        INCREASE_BRIGHTNESS(value = "appwidget_increase_brightness_"),
        VIEW_ORIGINAL_PHOTO(value = "appwidget_view_original_photo_"),
        NO_SHUFFLE(value = "appwidget_no_shuffle_"),
        KEEP_CURRENT_PHOTO(value = "appwidget_keep_current_photo_"),
        APP_SHORTCUT_LEFT(value = "appwidget_app_shortcut_left_"),
        APP_SHORTCUT_CENTER(value = "appwidget_app_shortcut_"),
        APP_SHORTCUT_RIGHT(value = "appwidget_app_shortcut_right_"),
        APP_FOLDER_LEFT(value = "appwidget_app_folder_left_"),
        APP_FOLDER_CENTER(value = "appwidget_app_folder_"),
        APP_FOLDER_RIGHT(value = "appwidget_app_folder_right_"),
        URL_SHORTCUT_LEFT(value = "appwidget_url_shortcut_left_"),
        URL_SHORTCUT_CENTER(value = "appwidget_url_shortcut_"),
        URL_SHORTCUT_RIGHT(value = "appwidget_url_shortcut_right_"),
        PREFERRED_GALLERY_APP(value = "appwidget_preferred_gallery_app_"),
        DISABLE_TAP(value = "appwidget_disable_tap_"),

        TEXT_TYPE(value = "appwidget_text_type_"),
        TEXT_VALUE(value = "appwidget_text_value_"),
        TEXT_SIZE(value = "appwidget_text_size_"),
        TEXT_VERTICAL_OFFSET(value = "appwidget_text_vertical_offset_"),
        TEXT_HAS_SHADOW(value = "appwidget_text_has_shadow_"),

        DELETION_TIMESTAMP(value = "appwidget_deletion_timestamp_"),
        ;

        override fun toString(): String = value
    }

    private companion object {

        const val SHARED_PREFERENCES_NAME = "com.fibelatti.photowidget.PhotoWidget"
    }
}
