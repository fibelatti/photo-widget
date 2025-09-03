package com.fibelatti.photowidget.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.backup.PhotoWidgetBackupActivity
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.configure.aspectRatio
import com.fibelatti.photowidget.configure.sharedPhotos
import com.fibelatti.photowidget.hints.HintStorage
import com.fibelatti.photowidget.licenses.OssLicensesActivity
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetStatus
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.photowidget.platform.widgetPinningNotAvailable
import com.fibelatti.photowidget.preferences.Appearance
import com.fibelatti.photowidget.preferences.DataSaverPicker
import com.fibelatti.photowidget.preferences.UserPreferencesStorage
import com.fibelatti.photowidget.preferences.WidgetDefaultsActivity
import com.fibelatti.photowidget.ui.Toggle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val homeViewModel by viewModels<HomeViewModel>()

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    @Inject
    lateinit var hintStorage: HintStorage

    private var preparedIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val currentWidgets by homeViewModel.currentWidgets.collectAsStateWithLifecycle()
                var showBackgroundRestrictionHint by remember {
                    mutableStateOf(hintStorage.showHomeBackgroundRestrictionsHint)
                }

                HomeScreen(
                    onCreateNewWidgetClick = ::createNewWidget,
                    currentWidgets = currentWidgets,
                    onCurrentWidgetClick = ::showExistingWidgetMenu,
                    onRemovedWidgetClick = ::showRemovedWidgetMenu,
                    onDefaultsClick = ::showDefaults,
                    onDataSaverClick = ::showDataSaverPicker,
                    onAppearanceClick = ::showAppearancePicker,
                    onColorsClick = ::showAppColorsPicker,
                    onAppLanguageClick = ::showTranslationsDialog,
                    onBackupClick = {
                        startActivity(PhotoWidgetBackupActivity.newIntent(this))
                    },
                    onRateClick = ::rateApp,
                    onShareClick = ::shareApp,
                    showBackgroundRestrictionHint = showBackgroundRestrictionHint,
                    onDismissWarningClick = {
                        hintStorage.showHomeBackgroundRestrictionsHint = false
                        showBackgroundRestrictionHint = false
                    },
                    onPrivacyPolicyClick = ::openPrivacyPolicy,
                    onViewLicensesClick = ::viewOpenSourceLicenses,
                )
            }
        }

        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadCurrentWidgets()
    }

    @Suppress("DEPRECATION")
    private fun checkIntent() {
        if (!intent.hasExtra(Intent.EXTRA_STREAM)) return

        preparedIntent = PhotoWidgetConfigureActivity.newWidgetIntent(
            context = this,
            sharedPhotos = when {
                Intent.ACTION_SEND == intent.action -> {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let(::listOf)
                }

                Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                    intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.mapNotNull { it as? Uri }
                }

                else -> null
            },
        )

        val size = preparedIntent?.sharedPhotos?.size ?: 0
        if (size == 0) {
            preparedIntent = null
            return
        }

        MaterialAlertDialogBuilder(this)
            .setMessage(resources.getQuantityString(R.plurals.photo_widget_home_share_received, size, size))
            .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
            .show()
    }

    private fun createNewWidget(aspectRatio: PhotoWidgetAspectRatio) {
        if (widgetPinningNotAvailable()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.photo_widget_home_pinning_not_supported_title)
                .setMessage(R.string.photo_widget_home_pinning_not_supported_message)
                .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                .show()

            return
        }

        val intent: Intent = preparedIntent?.apply { this.aspectRatio = aspectRatio }
            ?: PhotoWidgetConfigureActivity.newWidgetIntent(context = this, aspectRatio = aspectRatio)

        preparedIntent = null

        startActivity(intent)
    }

    private fun showExistingWidgetMenu(appWidgetId: Int, canSync: Boolean, canLock: Boolean, isLocked: Boolean) {
        preparedIntent?.let {
            val intent = it.apply { this.appWidgetId = appWidgetId }

            preparedIntent = null

            startActivity(intent)

            return
        }

        SelectionDialog.show(
            context = this,
            title = "",
            options = MyWidgetOptions.options(canSync = canSync, canLock = canLock, isLocked = isLocked),
            optionName = { option -> getString(option.label) },
            onOptionSelected = { option ->
                when (option) {
                    MyWidgetOptions.SYNC_PHOTOS -> {
                        homeViewModel.syncPhotos(appWidgetId = appWidgetId)

                        Toast.makeText(this, R.string.photo_widget_home_my_widget_syncing_feedback, Toast.LENGTH_SHORT)
                            .show()
                    }

                    MyWidgetOptions.EDIT -> {
                        val intent = PhotoWidgetConfigureActivity.editWidgetIntent(
                            context = this,
                            appWidgetId = appWidgetId,
                        )

                        startActivity(intent)
                    }

                    MyWidgetOptions.DUPLICATE -> {
                        val intent = PhotoWidgetConfigureActivity.duplicateWidgetIntent(
                            context = this,
                            appWidgetId = appWidgetId,
                        )

                        startActivity(intent)
                    }

                    MyWidgetOptions.LOCK -> homeViewModel.lockWidget(appWidgetId = appWidgetId)

                    MyWidgetOptions.UNLOCK -> homeViewModel.unlockWidget(appWidgetId = appWidgetId)
                }
            },
            footer = {
                if (canLock) {
                    Text(
                        text = stringResource(R.string.photo_widget_home_my_widget_lock_explainer),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
        )
    }

    private fun showRemovedWidgetMenu(appWidgetId: Int, status: PhotoWidgetStatus) {
        SelectionDialog.show(
            context = this,
            title = "",
            options = buildList {
                add(RemovedWidgetOptions.RESTORE)
                if (PhotoWidgetStatus.KEPT != status) {
                    add(RemovedWidgetOptions.KEEP)
                }
                add(RemovedWidgetOptions.DELETE)
            },
            optionName = { option -> getString(option.label) },
            onOptionSelected = { option ->
                when (option) {
                    RemovedWidgetOptions.RESTORE -> {
                        val intent = PhotoWidgetConfigureActivity.restoreWidgetIntent(
                            context = this,
                            appWidgetId = appWidgetId,
                        )

                        startActivity(intent)
                    }

                    RemovedWidgetOptions.KEEP -> {
                        homeViewModel.keepWidget(appWidgetId = appWidgetId)
                    }

                    RemovedWidgetOptions.DELETE -> {
                        homeViewModel.deleteWidget(appWidgetId = appWidgetId)
                    }
                }
            },
        )
    }

    private fun showDefaults() {
        startActivity(Intent(this, WidgetDefaultsActivity::class.java))
    }

    private fun showDataSaverPicker() {
        DataSaverPicker.show(context = this)
    }

    private fun showAppearancePicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_home_appearance),
            options = Appearance.entries,
            optionName = { appearance ->
                getString(
                    when (appearance) {
                        Appearance.FOLLOW_SYSTEM -> R.string.preferences_appearance_follow_system
                        Appearance.LIGHT -> R.string.preferences_appearance_light
                        Appearance.DARK -> R.string.preferences_appearance_dark
                    },
                )
            },
            onOptionSelected = { newAppearance ->
                userPreferencesStorage.appearance = newAppearance

                val mode = when (newAppearance) {
                    Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                AppCompatDelegate.setDefaultNightMode(mode)
            },
            footer = {
                Toggle(
                    title = stringResource(R.string.photo_widget_home_true_black_background),
                    checked = userPreferencesStorage.useTrueBlack,
                    onCheckedChange = { newValue -> userPreferencesStorage.useTrueBlack = newValue },
                    modifier = Modifier.padding(all = 16.dp),
                )
            },
        )
    }

    private fun showAppColorsPicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_home_dynamic_colors),
            options = listOf(true, false),
            optionName = { value ->
                getString(
                    if (value) {
                        R.string.preferences_dynamic_colors_enabled
                    } else {
                        R.string.preferences_dynamic_colors_disabled
                    },
                )
            },
            onOptionSelected = { newValue ->
                userPreferencesStorage.dynamicColors = newValue

                ActivityCompat.recreate(this)
            },
        )
    }

    private fun showTranslationsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.translations_dialog_title)
            .setMessage(R.string.translations_dialog_body)
            .setPositiveButton(R.string.translations_dialog_positive_action) { _, _ ->
                openUrl("https://crowdin.com/project/material-photo-widget")
            }
            .setNegativeButton(R.string.translations_dialog_negative_action) { _, _ -> }
            .show()
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setChooserTitle(R.string.share_title)
            .setText(getString(R.string.share_text, APP_URL))
            .startChooser()
    }

    private fun rateApp() {
        openUrl(url = APP_URL)
    }

    private fun openPrivacyPolicy() {
        openUrl(url = "https://www.fibelatti.com/privacy-policy/material-photo-widget")
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun viewOpenSourceLicenses() {
        startActivity(Intent(this, OssLicensesActivity::class.java))
    }

    private enum class MyWidgetOptions(
        @StringRes val label: Int,
    ) {

        SYNC_PHOTOS(label = R.string.photo_widget_home_my_widget_action_sync),
        EDIT(label = R.string.photo_widget_home_my_widget_action_edit),
        DUPLICATE(label = R.string.photo_widget_home_my_widget_action_duplicate),
        LOCK(label = R.string.photo_widget_home_my_widget_action_lock),
        UNLOCK(label = R.string.photo_widget_home_my_widget_action_unlock),
        ;

        companion object {

            fun options(canSync: Boolean, canLock: Boolean, isLocked: Boolean): List<MyWidgetOptions> = buildList {
                if (canSync) {
                    add(SYNC_PHOTOS)
                }

                add(EDIT)
                add(DUPLICATE)

                when {
                    canLock && isLocked -> add(UNLOCK)
                    canLock -> add(LOCK)
                }
            }
        }
    }

    private enum class RemovedWidgetOptions(
        @StringRes val label: Int,
    ) {

        RESTORE(label = R.string.photo_widget_home_removed_widget_action_restore),
        KEEP(label = R.string.photo_widget_home_removed_widget_action_keep),
        DELETE(label = R.string.photo_widget_home_removed_widget_action_delete),
    }

    private companion object {

        private const val APP_URL = "https://play.google.com/store/apps/details?id=com.fibelatti.photowidget"
    }
}
