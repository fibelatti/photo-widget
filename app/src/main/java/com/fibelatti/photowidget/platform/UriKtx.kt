package com.fibelatti.photowidget.platform

import android.content.ContentResolver
import android.content.Context
import android.content.UriPermission
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract

/**
 * Converts a tree [Uri] returned by `OpenDocumentTree` into the document [Uri] addressing the tree's
 * root folder (e.g. `content://…/tree/primary%3APictures/document/primary%3APictures`).
 *
 * This is the form file managers expect as the `data` of an `ACTION_VIEW` +
 * `vnd.android.document/directory` intent; being a `content://` [Uri], it is covered directly by
 * `FLAG_GRANT_READ_URI_PERMISSION`.
 */
fun Uri.toFolderDocumentUri(): Uri {
    return DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))
}

/**
 * Whether [uri] is covered by a read permission that the user granted through the system pickers: either
 * the [Uri] itself, or the tree it was resolved from when it addresses a document inside a picked folder.
 *
 * Only such [Uri]s may be forwarded to other apps with `FLAG_GRANT_READ_URI_PERMISSION`; anything else would
 * hand out this app's own access, including to its private `FileProvider`.
 */
fun Context.hasPersistedReadPermission(uri: Uri): Boolean {
    return contentResolver.persistedUriPermissions.any { permission: UriPermission ->
        permission.isReadPermission && (permission.uri == uri || permission.uri.isTreeOf(uri))
    }
}

private fun Uri.isTreeOf(uri: Uri): Boolean {
    return DocumentsContract.isTreeUri(this) &&
        DocumentsContract.isTreeUri(uri) &&
        authority == uri.authority &&
        DocumentsContract.getTreeDocumentId(this) == DocumentsContract.getTreeDocumentId(uri)
}

/**
 * Whether this is a `content://` [Uri] served by another app.
 *
 * [Uri]s received from other apps are read with this app's own access, so anything else (a `file://` path or
 * one of this app's own providers) could expose its private files.
 */
fun Uri.isForeignContentUri(context: Context): Boolean {
    if (scheme != ContentResolver.SCHEME_CONTENT) return false

    val authority: String = authority ?: return false
    val packageManager: PackageManager = context.packageManager
    val providerInfo: ProviderInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveContentProvider(authority, PackageManager.ComponentInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveContentProvider(authority, 0)
    }

    return providerInfo?.packageName != context.packageName
}
