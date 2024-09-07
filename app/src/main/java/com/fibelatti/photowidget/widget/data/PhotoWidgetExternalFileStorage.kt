package com.fibelatti.photowidget.widget.data

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.fibelatti.photowidget.model.LocalPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

class PhotoWidgetExternalFileStorage @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val contentResolver = context.contentResolver

    fun takePersistableUriPermission(dirUri: Set<Uri>) {
        val newDir = dirUri - contentResolver.persistedUriPermissions.map { it.uri }.toSet()
        for (dir in newDir) {
            contentResolver.takePersistableUriPermission(dir, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun isValidDir(dirUri: Uri): Boolean {
        Timber.d("Checking validity of selected dir: $dirUri")

        if (dirUri.toString().endsWith("DCIM%2FCamera", ignoreCase = true)) {
            return false
        }

        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            /* treeUri = */ dirUri,
            /* documentId = */ DocumentsContract.getTreeDocumentId(dirUri),
        )

        return try {
            // Traverse the directory structure to ensure that all folders contains less than the limit
            getPhotoCount(documentUri = documentUri, applyValidation = true)
            true
        } catch (_: InvalidDirException) {
            false
        }
    }

    suspend fun getPhotoCount(dirUri: Set<Uri>): Int = coroutineScope {
        dirUri.map { uri ->
            async {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    /* treeUri = */ uri,
                    /* documentId = */ DocumentsContract.getTreeDocumentId(uri),
                )
                getPhotoCount(documentUri = documentUri, applyValidation = false)
            }
        }.awaitAll().sum()
    }

    suspend fun getPhotos(
        dirUri: Set<Uri>,
        croppedPhotos: Map<String, LocalPhoto>,
    ): List<LocalPhoto> = coroutineScope {
        dirUri.map { uri ->
            async {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    /* treeUri = */ uri,
                    /* documentId = */ DocumentsContract.getTreeDocumentId(uri),
                )
                getPhotos(documentUri = documentUri, croppedPhotos = croppedPhotos)
            }
        }.awaitAll().flatten()
    }

    private suspend fun getPhotoCount(documentUri: Uri, applyValidation: Boolean): Int {
        return usingCursor(documentUri = documentUri) { cursor ->
            var count = 0

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val mimeType = cursor.getString(1)
                val documentName = cursor.getString(2).takeUnless { it.startsWith(".trashed") }
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, documentId)

                Timber.d(
                    "Cursor item (" +
                        "documentId=$documentId, " +
                        "mimeType=$mimeType, " +
                        "documentName=$documentName, " +
                        "fileUri=$fileUri" +
                        ")",
                )

                if (documentName != null && mimeType in ALLOWED_TYPES && fileUri != null) {
                    count += 1
                } else if (documentName?.startsWith(".") != true && mimeType == "vnd.android.document/directory") {
                    val dirCount = getPhotoCount(documentUri = fileUri, applyValidation = applyValidation)

                    if (applyValidation && dirCount >= 3_000) throw InvalidDirException()

                    count += dirCount
                }
            }

            count
        } ?: 0
    }

    private suspend fun getPhotos(documentUri: Uri, croppedPhotos: Map<String, LocalPhoto>): List<LocalPhoto> {
        return usingCursor(documentUri = documentUri) { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val mimeType = cursor.getString(1)
                    val documentName = cursor.getString(2).takeUnless { it.startsWith(".trashed") }
                    val documentLastModified = cursor.getLong(3)
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, documentId)

                    Timber.d(
                        "Cursor item (" +
                            "documentId=$documentId, " +
                            "mimeType=$mimeType, " +
                            "documentName=$documentName, " +
                            "fileUri=$fileUri",
                    )

                    if (documentName != null && mimeType in ALLOWED_TYPES && fileUri != null) {
                        val localPhoto = LocalPhoto(
                            name = documentName,
                            path = croppedPhotos[documentName]?.path,
                            externalUri = fileUri,
                            timestamp = documentLastModified,
                        )

                        add(localPhoto)
                    } else if (documentName?.startsWith(".") != true && mimeType == "vnd.android.document/directory") {
                        addAll(getPhotos(documentUri = fileUri, croppedPhotos = croppedPhotos))
                    }
                }
            }.sortedByDescending { it.timestamp }
        }.orEmpty()
    }

    private suspend inline fun <T> usingCursor(documentUri: Uri, crossinline block: suspend (Cursor) -> T): T? {
        return withContext(Dispatchers.IO) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                /* treeUri = */ documentUri,
                /* parentDocumentId = */ DocumentsContract.getDocumentId(documentUri),
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )

            contentResolver.query(
                /* uri = */ childrenUri,
                /* projection = */ projection,
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )?.use { cursor -> block(cursor) }
        }
    }

    private class InvalidDirException : RuntimeException()

    private companion object {

        private val ALLOWED_TYPES = arrayOf("image/jpeg", "image/png")
    }
}
