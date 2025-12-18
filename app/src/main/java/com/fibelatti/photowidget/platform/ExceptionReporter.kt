package com.fibelatti.photowidget.platform

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

class ExceptionReporter @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
) {

    private val parentDir: File by lazy {
        File("${context.filesDir}/crashes").apply { mkdirs() }
    }

    fun collectReport(throwable: Throwable) {
        coroutineScope.launch(NonCancellable) {
            withContext(Dispatchers.IO) {
                val stringWriter = StringWriter()

                throwable.printStackTrace(PrintWriter(stringWriter))

                val timestamp: String = Clock.System.now()
                    .toLocalDateTime(timeZone = TimeZone.UTC)
                    .format(format = LocalDateTime.Formats.ISO)

                val file: File = File(parentDir, "$timestamp.txt").apply { createNewFile() }

                file.writeText(stringWriter.toString())
            }
        }
    }

    suspend fun getPendingReports(): List<File> {
        return withContext(Dispatchers.IO) {
            parentDir.listFiles()?.toList().orEmpty()
        }
    }

    fun clearPendingReports() {
        coroutineScope.launch(NonCancellable) {
            withContext(Dispatchers.IO) {
                parentDir.listFiles()?.forEach { it.delete() }
            }
        }
    }
}
