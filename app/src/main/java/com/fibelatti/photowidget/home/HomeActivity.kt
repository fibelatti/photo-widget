package com.fibelatti.photowidget.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.aspectRatio
import com.fibelatti.photowidget.configure.sharedPhotos
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.widgetPinningNotAvailable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private var preparedIntent: Intent? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    preparedIntent = preparedIntent,
                    onIntentConsumed = { preparedIntent = null },
                    onCreateNewWidgetClick = ::createNewWidget,
                    onAppLanguageClick = ::showTranslationsDialog,
                    onShareClick = ::shareApp,
                )
            }
        }

        checkIntent()

        homeViewModel.pendingReport
            .filterNotNull()
            .onEach(::showCrashReportDialog)
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadWidgets()
        homeViewModel.checkForPendingExceptionReports()
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

    private fun showTranslationsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.translations_dialog_title)
            .setMessage(R.string.translations_dialog_body)
            .setPositiveButton(R.string.translations_dialog_positive_action) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, "https://crowdin.com/project/material-photo-widget".toUri()))
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

    private fun showCrashReportDialog(reportText: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.photo_widget_home_crash_report_title))
            .setMessage(getString(R.string.photo_widget_home_crash_report_body))
            .setPositiveButton(getString(R.string.photo_widget_home_crash_report_action_confirm)) { dialog, _ ->
                val emailBody = buildString {
                    appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine()
                    append(reportText)
                    appendLine()
                }

                val emailIntent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("appsupport@fibelatti.com"))
                    .putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Material Photo Widget (${BuildConfig.VERSION_NAME}) — Crash Report",
                    )
                    .putExtra(Intent.EXTRA_TEXT, emailBody)

                startActivity(
                    Intent.createChooser(
                        emailIntent,
                        getString(R.string.photo_widget_home_crash_report_choose_title),
                    ),
                )

                homeViewModel.clearPendingExceptionReports()
                dialog?.dismiss()
            }
            .setNegativeButton(getString(R.string.photo_widget_home_crash_report_action_cancel)) { dialog, _ ->
                homeViewModel.clearPendingExceptionReports()
                dialog?.dismiss()
            }
            .setOnDismissListener {
                homeViewModel.clearPendingExceptionReports()
            }
            .show()
    }

    private companion object {

        private const val APP_URL = "https://play.google.com/store/apps/details?id=com.fibelatti.photowidget"
    }
}
