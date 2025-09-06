package com.fibelatti.photowidget.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.widgetPinningNotAvailable
import com.fibelatti.photowidget.preferences.WidgetDefaultsActivity
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val homeViewModel by viewModels<HomeViewModel>()

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

                val existingWidgetMenuSheetState = rememberAppSheetState()
                val removedWidgetSheetState = rememberAppSheetState()
                val appAppearanceSheetState = rememberAppSheetState()
                val appColorsSheetState = rememberAppSheetState()

                HomeScreen(
                    onCreateNewWidgetClick = ::createNewWidget,
                    currentWidgets = currentWidgets,
                    onCurrentWidgetClick = click@{ appWidgetId, canSync, canLock, isLocked ->
                        preparedIntent?.let {
                            val intent = it.apply { this.appWidgetId = appWidgetId }

                            preparedIntent = null

                            startActivity(intent)

                            return@click
                        }

                        existingWidgetMenuSheetState.showBottomSheet(
                            data = ExistingWidgetMenuBottomSheetData(
                                appWidgetId = appWidgetId,
                                canSync = canSync,
                                canLock = canLock,
                                isLocked = isLocked,
                            ),
                        )
                    },
                    onRemovedWidgetClick = { appWidgetId, photoWidgetStatus ->
                        removedWidgetSheetState.showBottomSheet(
                            data = RemovedWidgetBottomSheetData(
                                appWidgetId = appWidgetId,
                                status = photoWidgetStatus,
                            ),
                        )
                    },
                    onDefaultsClick = ::showDefaults,
                    onAppearanceClick = appAppearanceSheetState::showBottomSheet,
                    onColorsClick = appColorsSheetState::showBottomSheet,
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

                ExistingWidgetMenuBottomSheet(
                    sheetState = existingWidgetMenuSheetState,
                    onSync = homeViewModel::syncPhotos,
                    onLock = homeViewModel::lockWidget,
                    onUnlock = homeViewModel::unlockWidget,
                )

                RemovedWidgetBottomSheet(
                    sheetState = removedWidgetSheetState,
                    onKeep = homeViewModel::keepWidget,
                    onDelete = homeViewModel::deleteWidget,
                )

                AppAppearanceBottomSheet(
                    sheetState = appAppearanceSheetState,
                )

                AppColorsBottomSheet(
                    sheetState = appColorsSheetState,
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

    private fun showDefaults() {
        startActivity(Intent(this, WidgetDefaultsActivity::class.java))
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

    private companion object {

        private const val APP_URL = "https://play.google.com/store/apps/details?id=com.fibelatti.photowidget"
    }
}
