package com.fibelatti.photowidget.platform

import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

suspend inline fun File.runWithFileOutputStream(crossinline operation: (FileOutputStream) -> Unit) {
    withContext(Dispatchers.IO) {
        if (parentFile?.exists() == false) parentFile?.mkdirs()
        if (!exists()) createNewFile()

        runCatching {
            FileOutputStream(this@runWithFileOutputStream).use(operation)
            require(length() > 0) { "File is empty" }
        }.onSuccess {
            Timber.d("Successfully operated on $this")
        }.onFailure {
            Timber.e(it, "runWithFileOutputStream failed on $path.")
            delete()
        }
    }
}
