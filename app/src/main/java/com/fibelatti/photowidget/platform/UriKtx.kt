package com.fibelatti.photowidget.platform

import android.net.Uri
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
