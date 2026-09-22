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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.fibelatti.photowidget.BuildConfig
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.aspectRatio
import com.fibelatti.photowidget.configure.sharedPhotos
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.showMaterialAlertDialog
import com.fibelatti.photowidget.platform.widgetPinningNotAvailable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private var preparedIntent: Intent? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                HomeScreenNavDisplay(
                    homeViewModel = homeViewModel,
                    preparedIntent = preparedIntent,
                    onIntentConsume = { preparedIntent = null },
                    onCreateNewWidgetClick = ::createNewWidget,
                    onCreateTransparentWidgetClick = ::createTransparentWidget,
                    onRestoreWidgetClick = ::restoreWidget,
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
            sharedPhotos = when (intent.action) {
                Intent.ACTION_SEND -> {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let(::listOf)
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>()
                }

                else -> null
            },
        )

        val size = preparedIntent?.sharedPhotos?.size ?: 0
        if (size == 0) {
            preparedIntent = null
            return
        }

        showMaterialAlertDialog {
            setMessage(resources.getQuantityString(R.plurals.photo_widget_home_share_received, size, size))
            setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
        }
    }

    private fun createNewWidget(aspectRatio: PhotoWidgetAspectRatio) {
        if (widgetPinningNotAvailable()) {
            showMaterialAlertDialog {
                setTitle(R.string.photo_widget_home_pinning_not_supported_title)
                setMessage(R.string.photo_widget_home_pinning_not_supported_message)
                setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
            }

            return
        }

        val intent: Intent = preparedIntent?.apply { this.aspectRatio = aspectRatio }
            ?: PhotoWidgetConfigureActivity.newWidgetIntent(context = this, aspectRatio = aspectRatio)

        preparedIntent = null

        startActivity(intent)
    }

    private fun createTransparentWidget() {
        if (widgetPinningNotAvailable()) {
            showMaterialAlertDialog {
                setTitle(R.string.photo_widget_home_pinning_not_supported_title)
                setMessage(R.string.photo_widget_home_pinning_not_supported_message)
                setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
            }

            return
        }

        startActivity(PhotoWidgetConfigureActivity.newTransparentWidgetIntent(context = this))
    }

    private fun restoreWidget(photoWidget: PhotoWidget) {
        val intent: Intent = PhotoWidgetConfigureActivity.importWidgetIntent(
            context = this,
            photoWidget = photoWidget,
        )
        startActivity(intent)
    }

    private fun showTranslationsDialog() {
        showMaterialAlertDialog {
            setTitle(R.string.translations_dialog_title)
            setMessage(R.string.translations_dialog_body)
            setPositiveButton(R.string.translations_dialog_positive_action) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, "https://crowdin.com/project/material-photo-widget".toUri()))
            }
            setNegativeButton(R.string.translations_dialog_negative_action) { _, _ -> }
        }
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setChooserTitle(R.string.share_title)
            .setText(getString(R.string.share_text, APP_URL))
            .startChooser()
    }

    private fun showCrashReportDialog(pendingReport: PendingReport) {
        showMaterialAlertDialog {
            setTitle(getString(R.string.photo_widget_home_crash_report_title))
            setMessage(getString(R.string.photo_widget_home_crash_report_body))
            setPositiveButton(getString(R.string.photo_widget_home_crash_report_action_confirm)) { dialog, _ ->
                shareReport(pendingReport = pendingReport)
                homeViewModel.clearPendingExceptionReports()
                dialog?.dismiss()
            }
            setNegativeButton(getString(R.string.photo_widget_home_crash_report_action_cancel)) { dialog, _ ->
                homeViewModel.clearPendingExceptionReports()
                dialog?.dismiss()
            }
            setOnDismissListener {
                homeViewModel.clearPendingExceptionReports()
            }
        }
    }

    private fun shareReport(pendingReport: PendingReport) {
        val emailBody = buildString {
            appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine()
            append(pendingReport.text)
            appendLine()
        }

        val logUris: ArrayList<Uri> = pendingReport.logFiles.mapTo(ArrayList()) { file ->
            FileProvider.getUriForFile(this@HomeActivity, "$packageName.fileprovider", file)
        }

        val emailIntent: Intent = if (logUris.isEmpty()) {
            Intent(Intent.ACTION_SENDTO, MAILTO_URI)
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("text/plain")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, logUris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        emailIntent
            .putExtra(Intent.EXTRA_TEXT, emailBody)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf("appsupport@fibelatti.com"))
            .putExtra(
                Intent.EXTRA_SUBJECT,
                "Material Photo Widget (${BuildConfig.VERSION_NAME}) \u2014 Crash Report",
            )

        val intent: Intent? = if (logUris.isEmpty()) emailIntent else emailAppsIntent(emailIntent)

        if (intent == null) {
            Timber.e("No email app is available to share the report.")
            return
        }

        runCatching { startActivity(intent) }
            .onFailure { throwable -> Timber.e(throwable, "Unable to start the email intent.") }
    }

    /**
     * Attachments require `ACTION_SEND_MULTIPLE`, which every sharing target accepts, so the intent
     * is restricted to the apps that also handle `mailto` links to keep the report going to an
     * email app.
     */
    private fun emailAppsIntent(emailIntent: Intent): Intent? {
        val emailPackages: Set<String> = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_SENDTO, MAILTO_URI), 0)
            .mapTo(mutableSetOf()) { resolveInfo -> resolveInfo.activityInfo.packageName }

        val intents: List<Intent> = packageManager
            .queryIntentActivities(emailIntent, 0)
            .map { resolveInfo -> resolveInfo.activityInfo.packageName }
            .filter { packageName -> packageName in emailPackages }
            .distinct()
            .map { packageName -> Intent(emailIntent).setPackage(packageName) }

        return when (intents.size) {
            0 -> null

            1 -> intents.first()

            else -> Intent.createChooser(
                intents.first(),
                getString(R.string.photo_widget_home_crash_report_choose_title),
            ).putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(n = 1).toTypedArray())
        }
    }

    private companion object {

        private val MAILTO_URI: Uri = "mailto:".toUri()

        private const val APP_URL = "https://play.google.com/store/apps/details?id=com.fibelatti.photowidget"
    }
}
