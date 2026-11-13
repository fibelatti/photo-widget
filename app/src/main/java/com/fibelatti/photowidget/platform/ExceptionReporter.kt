package com.fibelatti.photowidget.platform

import android.content.Context
import android.os.DeadSystemException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeoutException
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
        coroutineScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                writeReport(throwable = throwable)
            }
        }
    }

    /**
     * Writes the report on the calling thread, for callers that cannot outlive the file being
     * written.
     */
    fun collectReportBlocking(throwable: Throwable) {
        runCatching { writeReport(throwable = throwable) }
    }

    private fun writeReport(throwable: Throwable) {
        val stringWriter = StringWriter()

        throwable.printStackTrace(PrintWriter(stringWriter))

        val timestamp: String = Clock.System.now()
            .toLocalDateTime(timeZone = TimeZone.UTC)
            .format(format = LocalDateTime.Formats.ISO)

        val file: File = File(parentDir, "$timestamp.txt").apply { createNewFile() }

        file.writeText(stringWriter.toString())
    }

    suspend fun getPendingReports(): List<File> {
        return withContext(Dispatchers.IO) {
            parentDir.listFiles()?.toList().orEmpty()
        }
    }

    fun clearPendingReports() {
        coroutineScope.launch {
            withContext(NonCancellable + Dispatchers.IO) {
                parentDir.listFiles()?.forEach { it.delete() }
            }
        }
    }
}

fun Throwable.skipReporting(): Boolean {
    return isDeadSystem() || isAbandonedJobCleanupTimeout()
}

/**
 * Whether the failure comes from the system server having died, which takes every app process
 * down with it.
 *
 * Binder calls rethrow it wrapped in a hidden runtime exception, so the cause chain is checked
 * for the public [DeadSystemException].
 */
private fun Throwable.isDeadSystem(): Boolean {
    return generateSequence(this) { it.cause }.any { it is DeadSystemException }
}

/**
 * Whether this is the finalizer watchdog killing the process because `JobScheduler` did not
 * answer the abandoned-job check within its timeout.
 *
 * The platform runs that check when the parameters of a stopped job are garbage collected, and
 * `WorkManager` drops them on stop as the `JobService` contract allows. The blocking call only
 * times out when the system itself is unresponsive, typically while the device shuts down.
 */
private fun Throwable.isAbandonedJobCleanupTimeout(): Boolean {
    return this is TimeoutException &&
        stackTrace.any { it.className == $$"android.app.job.JobParameters$JobCleanupCallback" }
}
