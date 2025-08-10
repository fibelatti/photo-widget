package com.fibelatti.photowidget.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibelatti.photowidget.model.PhotoWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PhotoWidgetBackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
) : ViewModel() {

    var state: State by mutableStateOf(State())
        private set

    fun createBackup() {
        viewModelScope.launch {
            state = state.copy(isProcessing = true)
            try {
                val backup: File = createBackupUseCase()
                state = state.copy(isProcessing = false, preparedBackupFile = backup)
            } catch (_: Exception) {
                state = state.copy(isProcessing = false, messages = state.messages + State.Message.BackupFailed)
            }
        }
    }

    fun exportBackup(destinationUri: Uri) {
        val preparedBackupFile: File = state.preparedBackupFile ?: return

        viewModelScope.launch {
            val message: State.Message = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        preparedBackupFile.inputStream().buffered().use { bufferedInputStream ->
                            bufferedInputStream.copyTo(outputStream)
                        }
                    }
                }
            }.fold(
                onSuccess = { State.Message.BackupExportedSuccessfully },
                onFailure = { State.Message.BackupFailed },
            )

            state = state.copy(messages = state.messages + message)
            deletePreparedBackup()
        }
    }

    fun restoreFromBackup(uri: Uri) {
        viewModelScope.launch {
            state = state.copy(isProcessing = true)

            try {
                val (restoredFile: File?, widgets: List<PhotoWidget>) = restoreBackupUseCase(uri = uri)
                state = state.copy(
                    isProcessing = false,
                    restoredBackupFile = restoredFile,
                    restoredWidgets = widgets,
                )
            } catch (_: Exception) {
                state = state.copy(
                    isProcessing = false,
                    restoredBackupFile = null,
                    restoredWidgets = emptyList(),
                    messages = state.messages + State.Message.RestoreBackupFailed,
                )
            }
        }
    }

    fun deletePreparedBackup() {
        state.preparedBackupFile?.deleteRecursively()
        state = state.copy(preparedBackupFile = null)
    }

    fun deleteRestoredBackup() {
        state.restoredBackupFile?.deleteRecursively()
        state = state.copy(restoredBackupFile = null)
    }

    fun messageHandled(message: State.Message) {
        state = state.copy(messages = state.messages - message)
    }

    data class State(
        val isProcessing: Boolean = false,
        val preparedBackupFile: File? = null,
        val restoredBackupFile: File? = null,
        val restoredWidgets: List<PhotoWidget> = emptyList(),
        val messages: List<Message> = emptyList(),
    ) {

        sealed class Message {

            data object BackupExportedSuccessfully : Message()

            data object BackupFailed : Message()

            data object RestoreBackupFailed : Message()
        }
    }
}
