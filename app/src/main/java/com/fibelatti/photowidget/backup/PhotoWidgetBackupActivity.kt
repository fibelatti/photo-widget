package com.fibelatti.photowidget.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.RememberedEffect
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class PhotoWidgetBackupActivity : AppCompatActivity() {

    private val backupViewModel by viewModels<PhotoWidgetBackupViewModel>()

    private val saveFileLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        callback = ::onExportDestinationSelected,
    )

    private val backupPickerLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        callback = ::onBackupFileSelected,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val state: PhotoWidgetBackupViewModel.State = backupViewModel.state

                PhotoWidgetBackupScreen(
                    onNavClick = ::finish,
                    isProcessing = state.isProcessing,
                    onCreateBackupClick = backupViewModel::createBackup,
                    onRestoreFromBackupClick = { backupPickerLauncher.launch("application/zip") },
                    widgets = state.restoredWidgets,
                    onRestoreClick = { photoWidget ->
                        val intent = PhotoWidgetConfigureActivity.importWidgetIntent(
                            context = this,
                            photoWidget = photoWidget,
                        )
                        startActivity(intent)
                    },
                )

                RememberedEffect(state.preparedBackupFile) {
                    state.preparedBackupFile?.let(::launchDestinationChooser)
                }

                RememberedEffect(state.messages) {
                    state.messages.firstOrNull()?.let(::handleMessage)
                }
            }
        }
    }

    override fun onDestroy() {
        backupViewModel.deleteRestoredBackup()
        super.onDestroy()
    }

    private fun launchDestinationChooser(preparedBackupFile: File) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, preparedBackupFile.name)
        }
        saveFileLauncher.launch(intent)
    }

    private fun onExportDestinationSelected(result: ActivityResult) {
        val destinationUri: Uri? = result.data?.data
        if (result.resultCode == RESULT_OK && destinationUri != null) {
            backupViewModel.exportBackup(destinationUri = destinationUri)
        } else {
            backupViewModel.deletePreparedBackup()
        }
    }

    private fun onBackupFileSelected(uri: Uri?) {
        if (uri != null) backupViewModel.restoreFromBackup(uri)
    }

    private fun handleMessage(message: PhotoWidgetBackupViewModel.State.Message) {
        when (message) {
            is PhotoWidgetBackupViewModel.State.Message.BackupExportedSuccessfully -> {
                Toast.makeText(this, R.string.backup_feedback_success, Toast.LENGTH_SHORT)
                    .show()
            }

            is PhotoWidgetBackupViewModel.State.Message.BackupFailed -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.backup_feedback_error)
                    .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    .show()
            }

            is PhotoWidgetBackupViewModel.State.Message.RestoreBackupFailed -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.backup_feedback_restore_error)
                    .setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    .show()
            }
        }

        backupViewModel.messageHandled(message)
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, PhotoWidgetBackupActivity::class.java)
        }
    }
}
