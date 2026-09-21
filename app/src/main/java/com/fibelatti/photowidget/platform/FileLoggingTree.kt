package com.fibelatti.photowidget.platform

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

@Singleton
class FileLoggingTree @Inject constructor(
    @ApplicationContext context: Context,
) : Timber.Tree() {

    private val parentDir: File by lazy {
        File("${context.filesDir}/logs").apply { mkdirs() }
    }

    private val currentFile: File get() = File(parentDir, CURRENT_FILE_NAME)

    private val previousFile: File get() = File(parentDir, PREVIOUS_FILE_NAME)

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FileLoggingTree").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val timestamp: String = Clock.System.now()
            .toLocalDateTime(timeZone = TimeZone.currentSystemDefault())
            .format(format = LocalDateTime.Formats.ISO)

        val entry: String = buildString {
            append(timestamp)
            append(' ')
            append(priorityLabel(priority))
            if (tag != null) {
                append('/')
                append(tag)
            }
            append(": ")
            append(message)
            if (t != null) {
                appendLine()
                val stringWriter = StringWriter()
                t.printStackTrace(PrintWriter(stringWriter))
                append(stringWriter.toString().trimEnd())
            }
            appendLine()
        }

        // The timestamp is taken above, on the calling thread, so queueing does not distort it.
        submit { write(entry = entry) }
    }

    private fun write(entry: String) {
        runCatching {
            val file: File = currentFile

            if (file.length() + entry.length >= MAX_FILE_SIZE_BYTES) {
                previousFile.delete()
                // Dropping the current file keeps the cap enforced when the rotation fails.
                if (!file.renameTo(previousFile)) file.delete()
            }

            file.appendText(entry)
        }
    }

    /**
     * Returns the log files oldest first, so that concatenating them yields a single chronological
     * log. Empty when nothing has been logged yet.
     *
     * Entries still queued are written out first, so a report collected right after a failure
     * includes the entries describing it.
     */
    suspend fun getLogFiles(): List<File> = withContext(Dispatchers.IO) {
        flushPendingEntries()
        listOf(previousFile, currentFile).filter { it.exists() && it.length() > 0 }
    }

    private fun submit(action: () -> Unit) {
        // The executor rejects work once the process is shutting down, dropping the entry.
        runCatching { executor.execute(action) }
    }

    /**
     * Blocks until the queued entries have been written, for callers that cannot outlive the
     * queue being flushed.
     */
    fun flushPendingEntries() {
        // Reporting a slightly shorter log is better than making the caller wait on the queue.
        runCatching { executor.submit { }.get(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    private fun priorityLabel(priority: Int): Char = when (priority) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }

    private companion object {

        const val CURRENT_FILE_NAME = "log.txt"
        const val PREVIOUS_FILE_NAME = "log-previous.txt"
        const val MAX_FILE_SIZE_BYTES: Int = 1024 * 1024
        const val FLUSH_TIMEOUT_SECONDS: Long = 2
    }
}
