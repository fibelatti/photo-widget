package com.fibelatti.photowidget.platform

import java.io.File
import java.io.FileOutputStream
import timber.log.Timber

fun File.runWithFileOutputStream(operation: (FileOutputStream) -> Unit) {
    if (!exists()) createNewFile()

    runCatching {
        FileOutputStream(this).use(operation)
        require(this.length() > 0) { "File is empty" }
    }.onSuccess {
        Timber.d("Successfully operated on $this")
    }.onFailure {
        Timber.e(it, "runWithFileOutputStream failed on $path.")
        delete()
    }
}
