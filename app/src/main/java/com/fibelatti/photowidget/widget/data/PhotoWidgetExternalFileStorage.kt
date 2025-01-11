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

    suspend fun getPhotos(
        dirUri: Set<Uri>,
        croppedPhotos: Map<String, LocalPhoto>,
        applyValidation: Boolean = false,
    ): List<LocalPhoto> = coroutineScope {
        dirUri.map { uri ->
            async {
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    /* treeUri = */
                    uri,
                    /* documentId = */
                    DocumentsContract.getTreeDocumentId(uri),
                )
                getPhotos(documentUri = documentUri, croppedPhotos = croppedPhotos, applyValidation = applyValidation)
            }
        }.awaitAll().flatten()
    }

    private suspend fun getPhotos(
        documentUri: Uri,
        croppedPhotos: Map<String, LocalPhoto>,
        applyValidation: Boolean = false,
    ): List<LocalPhoto> {
        return usingCursor(documentUri = documentUri) { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val mimeType = cursor.getString(1)
                    val documentName = cursor.getString(2).takeUnless { it.startsWith(".trashed") }
                    val documentLastModified = cursor.getLong(3)
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(documentUri, documentId)
                    val photoId = documentName?.let {
                        createPhotoId(documentId = documentId, documentName = documentName)
                    }

                    Timber.d(
                        "Cursor item (" +
                            "documentId=$documentId, " +
                            "mimeType=$mimeType, " +
                            "documentName=$documentName, " +
                            "fileUri=$fileUri, " +
                            "photoId=$photoId" +
                            ")",
                    )

                    if (photoId != null && mimeType in ALLOWED_TYPES && fileUri != null) {
                        val path = (croppedPhotos[photoId] ?: croppedPhotos[documentName])?.croppedPhotoPath
                        val localPhoto = LocalPhoto(
                            photoId = photoId,
                            croppedPhotoPath = path,
                            externalUri = fileUri,
                            timestamp = documentLastModified,
                        )

                        add(localPhoto)

                        if (applyValidation && size >= 3_000) {
                            throw InvalidDirException()
                        }
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
                /* treeUri = */
                documentUri,
                /* parentDocumentId = */
                DocumentsContract.getDocumentId(documentUri),
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )

            contentResolver.query(
                /* uri = */
                childrenUri,
                /* projection = */
                projection,
                /* selection = */
                null,
                /* selectionArgs = */
                null,
                /* sortOrder = */
                null,
            )?.use { cursor -> block(cursor) }
        }
    }

    /**
     * Creates a unique photo id for each photo, given their document id and name.
     *
     * Originally, the external document name was used as the internal id, but that can lead to
     * issues if two files with a common parent directory have the same file name. To avoid that,
     * the full path is used to create a unique id, replacing the usual separator with a custom
     * one in order to split it later, maintaining backwards compatibility.
     */
    private fun createPhotoId(documentId: String, documentName: String): String {
        return documentId.substringAfter(delimiter = ":", missingDelimiterValue = documentName)
            .replace(oldValue = "/", newValue = PhotoWidgetStorage.SEPARATOR)
    }

    private companion object {

        private val ALLOWED_TYPES = arrayOf("image/jpeg", "image/png")
    }
}

class InvalidDirException : RuntimeException()
