package com.fibelatti.photowidget.configure

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoCropActivity.Companion.outputPath
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.RememberedEffect
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.ui.foundation.AppSheetState
import com.fibelatti.ui.foundation.rememberAppSheetState
import com.fibelatti.ui.foundation.showBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PhotoWidgetConfigureActivity : AppCompatActivity() {

    private val viewModel by viewModels<PhotoWidgetConfigureViewModel>()

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
        ::onPhotoPicked,
    )

    private val photoCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onPhotoCropped,
    )

    private val photoDirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
        ::onDirPicked,
    )

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = false) {

        override fun handleOnBackPressed() {
            handleBackNav()
        }
    }

    private val finishReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Broadcast received (action=${intent.action})")

            Toast.makeText(
                this@PhotoWidgetConfigureActivity,
                R.string.photo_widget_configure_widget_pinned,
                Toast.LENGTH_SHORT,
            ).show()

            if (ACTION_FINISH == intent.action) {
                widgetAdded(appWidgetId = intent.appWidgetId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        setContent {
            AppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()

                val importFromWidgetSheetState = rememberAppSheetState()

                CompositionLocalProvider(LocalSamplePhoto provides state.selectedPhoto) {
                    PhotoWidgetConfigureScreen(
                        viewModel = viewModel,
                        isUpdating = intent.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID,
                        onNavClick = onBackPressedDispatcher::onBackPressed,
                        onPhotoPickerClick = ::launchPhotoPicker,
                        onDirPickerClick = ::launchFolderPicker,
                    )

                    ImportFromWidgetBottomSheet(
                        sheetState = importFromWidgetSheetState,
                        onWidgetSelected = viewModel::importFromWidget,
                    )
                }

                RememberedEffect(state.hasEdits) {
                    onBackPressedCallback.isEnabled = state.hasEdits
                }

                RememberedEffect(state.messages) {
                    state.messages.firstOrNull()?.let { message ->
                        handleMessage(
                            message = message,
                            importFromWidgetSheetState = importFromWidgetSheetState,
                        )
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            finishReceiver,
            IntentFilter(ACTION_FINISH),
        )

        checkIntent()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver)
        super.onDestroy()
    }

    private fun checkIntent() {
        intent.sharedPhotos?.let(viewModel::photoPicked)
    }

    private fun handleBackNav() {
        MaterialAlertDialogBuilder(this)
            .setMessage(
                if (intent.restoreFromId != null || intent.backupWidget != null) {
                    R.string.photo_widget_configure_navigate_back_warning_restore
                } else {
                    R.string.photo_widget_configure_navigate_back_warning
                },
            )
            .setPositiveButton(R.string.photo_widget_action_yes) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.photo_widget_action_no) { _, _ -> }
            .show()
    }

    private fun handleMessage(
        message: PhotoWidgetConfigureState.Message,
        importFromWidgetSheetState: AppSheetState,
    ) {
        when (message) {
            is PhotoWidgetConfigureState.Message.SuggestImport -> {
                showImportFromWidgetSuggestion(importFromWidgetSheetState)
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.ImportFailed -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.photo_widget_configure_import_error)
                    .setPositiveButton(R.string.photo_widget_action_continue) { _, _ -> }
                    .setOnDismissListener { viewModel.messageHandled(message = message) }
                    .show()
            }

            is PhotoWidgetConfigureState.Message.TooManyPhotos -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.photo_widget_configure_too_many_photos_error)
                    .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    .setOnDismissListener { viewModel.messageHandled(message = message) }
                    .show()
            }

            is PhotoWidgetConfigureState.Message.LaunchCrop -> {
                launchPhotoCrop(
                    sourceUri = message.source,
                    destinationUri = message.destination,
                    aspectRatio = message.aspectRatio,
                )
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.RequestPin -> {
                requestPin()
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.AddWidget -> {
                addNewWidget(appWidgetId = message.appWidgetId)
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.MissingPhotos -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.photo_widget_configure_missing_photos_error)
                    .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    .setOnDismissListener { viewModel.messageHandled(message = message) }
                    .show()
            }

            is PhotoWidgetConfigureState.Message.MissingBackupData -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.backup_feedback_restore_error)
                    .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    .setOnDismissListener { viewModel.messageHandled(message = message) }
                    .show()
            }

            is PhotoWidgetConfigureState.Message.CancelWidget -> {
                finish()
                viewModel.messageHandled(message = message)
            }
        }
    }

    private fun showImportFromWidgetSuggestion(
        importFromWidgetSheetState: AppSheetState,
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            R.string.photo_widget_configure_import_prompt,
            Snackbar.LENGTH_LONG,
        ).apply {
            setAction(R.string.photo_widget_configure_import_prompt_action) {
                importFromWidgetSheetState.showBottomSheet()
            }
        }.show()
    }

    private fun launchPhotoPicker() {
        photoPickerLauncher.launch("image/*")
    }

    private fun launchFolderPicker() {
        photoDirPickerLauncher.launch(null)
    }

    private fun onPhotoPicked(source: List<Uri>) {
        viewModel.photoPicked(source = source)
    }

    private fun onDirPicked(uri: Uri?) {
        viewModel.dirPicked(source = uri)
    }

    private fun launchPhotoCrop(
        sourceUri: Uri,
        destinationUri: Uri,
        aspectRatio: PhotoWidgetAspectRatio,
    ) {
        val intent = PhotoCropActivity.newIntent(
            context = this,
            sourceUri = sourceUri,
            destinationUri = destinationUri,
            aspectRatio = aspectRatio,
        )

        photoCropLauncher.launch(intent)
    }

    private fun onPhotoCropped(result: ActivityResult) {
        result.data?.outputPath
            ?.let(viewModel::photoCropped)
            ?: run { viewModel.cropCancelled() }
    }

    private fun addNewWidget(appWidgetId: Int) {
        widgetAdded(appWidgetId = appWidgetId)
        PhotoWidgetProvider.update(
            context = this,
            appWidgetId = appWidgetId,
        )
    }

    private fun widgetAdded(appWidgetId: Int) {
        viewModel.widgetAdded()

        val resultValue = Intent().apply {
            this.appWidgetId = appWidgetId
        }

        setResult(RESULT_OK, resultValue)

        finish()
    }

    private fun requestPin() {
        if (lifecycle.currentState != Lifecycle.State.RESUMED) {
            return
        }

        val callbackIntent = Intent(this, PhotoWidgetPinnedReceiver::class.java).apply {
            setIdentifierCompat("$PIN_REQUEST_CODE")
        }
        val successCallback = PendingIntent.getBroadcast(
            /* context = */ this,
            /* requestCode = */ PIN_REQUEST_CODE,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        Timber.d("Invoking AppWidgetManager#requestPinAppWidget")
        AppWidgetManager.getInstance(this).requestPinAppWidget(
            /* provider = */ ComponentName(this, PhotoWidgetProvider::class.java),
            /* extras = */ null,
            /* successCallback = */ successCallback,
        )
    }

    companion object {

        private const val PIN_REQUEST_CODE = 1001
        const val ACTION_FINISH = "FINISH_PHOTO_WIDGET_CONFIGURE_ACTIVITY"

        fun newWidgetIntent(
            context: Context,
            aspectRatio: PhotoWidgetAspectRatio? = null,
            sharedPhotos: List<Uri>? = null,
        ): Intent {
            return Intent(context, PhotoWidgetConfigureActivity::class.java).apply {
                if (aspectRatio != null) this.aspectRatio = aspectRatio
                if (sharedPhotos != null) this.sharedPhotos = sharedPhotos
            }
        }

        fun editWidgetIntent(context: Context, appWidgetId: Int): Intent {
            return Intent(context, PhotoWidgetConfigureActivity::class.java).apply {
                setIdentifierCompat("$appWidgetId")
                this.appWidgetId = appWidgetId
            }
        }

        fun duplicateWidgetIntent(context: Context, appWidgetId: Int): Intent {
            return Intent(context, PhotoWidgetConfigureActivity::class.java).apply {
                this.duplicateFromId = appWidgetId
            }
        }

        fun restoreWidgetIntent(context: Context, appWidgetId: Int): Intent {
            return Intent(context, PhotoWidgetConfigureActivity::class.java).apply {
                this.restoreFromId = appWidgetId
            }
        }

        fun importWidgetIntent(context: Context, photoWidget: PhotoWidget): Intent {
            return Intent(context, PhotoWidgetConfigureActivity::class.java).apply {
                this.backupWidget = photoWidget
            }
        }
    }
}
