package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.GoogleFontsLoader.Companion.WIDGET_GRACE_PERIOD
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Loads fonts from the Google Fonts provider, which ships with Google Play services.
 *
 * Callers must check [isAvailable] first since devices without the services can't load them.
 * [getTypeface] and [getWidgetTypeface] fall back to [Typeface.DEFAULT] for widgets configured
 * elsewhere (e.g. restored from a backup).
 */
@Singleton
class GoogleFontsLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) {

    private val cache: MutableMap<String, Typeface> = ConcurrentHashMap()

    /**
     * Requests made by [getWidgetTypeface] that are still in progress, shared by every widget that
     * uses the same font.
     */
    private val widgetRequests: MutableMap<String, Deferred<Typeface?>> = ConcurrentHashMap()

    /**
     * The provider gives each request 10 seconds to download its font before failing it, and then
     * keeps failing that font for a while. Requesting a whole list at once queues the downloads
     * past that limit, so requests are made one at a time.
     */
    private val catalogDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    /**
     * Requests for fonts being rendered have a queue of their own, so they don't wait behind the
     * requests of the whole catalog.
     */
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    /**
     * Returns true when Google Play services is available on this device and [getTypeface] can
     * return anything other than [Typeface.DEFAULT].
     */
    fun isAvailable(): Boolean {
        return context.packageManager.resolveContentProvider(PROVIDER_AUTHORITY, 0) != null
    }

    /**
     * The typeface of [fontFamily] if it has already been loaded, or `null` otherwise.
     */
    fun peekTypeface(fontFamily: String): Typeface? = cache[fontFamily]

    /**
     * The typeface of [fontFamily] used to render the text, or [Typeface.DEFAULT] when it's `null`
     * or fails to load. Waits for the request to finish, so widgets use [getWidgetTypeface] instead.
     */
    suspend fun getTypeface(fontFamily: String?): Typeface {
        if (fontFamily == null) return Typeface.DEFAULT

        return loadTypeface(fontFamily = fontFamily, dispatcher = renderDispatcher) ?: Typeface.DEFAULT
    }

    /**
     * The typeface of [fontFamily] used to render a widget, or [Typeface.DEFAULT] when it's `null`,
     * fails to load, or doesn't load within [WIDGET_GRACE_PERIOD].
     *
     * Fonts are only cached in memory, so the first render after the process starts has to request
     * them again. The grace period covers fonts the provider already has on the device, without
     * holding the widget for a download. When the grace period runs out, the request keeps going
     * and [onLateLoad] is invoked if it succeeds, so the widget can be rendered again with it.
     */
    suspend fun getWidgetTypeface(fontFamily: String?, onLateLoad: suspend () -> Unit): Typeface {
        if (fontFamily == null) return Typeface.DEFAULT
        cache[fontFamily]?.let { return it }

        val request: Deferred<Typeface?> = widgetRequests.computeIfAbsent(fontFamily) {
            coroutineScope.async(start = CoroutineStart.LAZY) {
                try {
                    loadTypeface(fontFamily = fontFamily, dispatcher = renderDispatcher)
                } finally {
                    @Suppress("DeferredResultUnused")
                    widgetRequests.remove(fontFamily)
                }
            }
        }
        request.start()

        // `null` only when the grace period runs out, since a failed request resolves to the default
        val typeface: Typeface? = withTimeoutOrNull(WIDGET_GRACE_PERIOD) {
            request.await() ?: Typeface.DEFAULT
        }
        if (typeface != null) return typeface

        coroutineScope.launch {
            if (request.await() != null) onLateLoad()
        }

        return Typeface.DEFAULT
    }

    /**
     * The typeface of [fontFamily], or `null` when it fails to load. Failures aren't cached, so
     * calling this again retries the request. Meant for loading the catalog, so these requests
     * yield to the ones made by [getTypeface] and [getWidgetTypeface].
     */
    suspend fun loadTypeface(fontFamily: String): Typeface? {
        return loadTypeface(fontFamily = fontFamily, dispatcher = catalogDispatcher)
    }

    private suspend fun loadTypeface(fontFamily: String, dispatcher: CoroutineDispatcher): Typeface? {
        cache[fontFamily]?.let { return it }

        return withContext(dispatcher) {
            // Another request for the same font may have completed while this one was queued
            cache[fontFamily] ?: fetchTypeface(fontFamily = fontFamily)?.also { cache[fontFamily] = it }
        }
    }

    private suspend fun fetchTypeface(fontFamily: String): Typeface? {
        val request = FontRequest(
            /* providerAuthority = */ PROVIDER_AUTHORITY,
            /* providerPackage = */ PROVIDER_PACKAGE,
            /* query = */ "name=$fontFamily",
            /* certificates = */ R.array.com_google_android_gms_fonts_certs,
        )

        val result: FontsContractCompat.FontFamilyResult = try {
            withCancellationSignal { cancellationSignal: CancellationSignal ->
                FontsContractCompat.fetchFonts(context, cancellationSignal, request)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            Timber.w("Google Fonts provider not found")
            return null
        } catch (e: OperationCanceledException) {
            throw CancellationException("Font request cancelled", e)
        }

        if (result.statusCode != FontsContractCompat.FontFamilyResult.STATUS_OK) {
            Timber.w(
                "Failed to fetch font %s",
                mapOf("fontFamily" to fontFamily, "status" to result.statusCode),
            )
            return null
        }

        // An unknown family name still reports STATUS_OK, with the failure only in each font's result code
        val fontResultCodes: List<Int> = result.fonts.map { it.resultCode }
        if (fontResultCodes.isEmpty() || fontResultCodes.any { it != FontsContractCompat.Columns.RESULT_CODE_OK }) {
            Timber.w(
                "Font not available %s",
                mapOf("fontFamily" to fontFamily, "resultCodes" to fontResultCodes),
            )
            return null
        }

        return FontsContractCompat.buildTypeface(context, null, result.fonts)
            .also { typeface ->
                if (typeface != null) {
                    Timber.d("Font loaded %s", mapOf("fontFamily" to fontFamily))
                } else {
                    Timber.w("Failed to build font %s", mapOf("fontFamily" to fontFamily))
                }
            }
    }

    /**
     * Runs the blocking [block] with a [CancellationSignal] that is canceled along with the
     * calling coroutine, so the provider stops the request instead of it holding the queue until
     * it times out.
     */
    private suspend fun <T> withCancellationSignal(block: (CancellationSignal) -> T): T = coroutineScope {
        val cancellationSignal = CancellationSignal()

        // Unconfined so the signal is canceled from the cancelling thread, since the thread running
        // the block stays blocked until the request returns
        val cancellationJob: Job = launch(Dispatchers.Unconfined) {
            try {
                awaitCancellation()
            } finally {
                cancellationSignal.cancel()
            }
        }

        try {
            block(cancellationSignal)
        } finally {
            cancellationJob.cancel()
        }
    }

    companion object {

        private const val PROVIDER_AUTHORITY: String = "com.google.android.gms.fonts"
        private const val PROVIDER_PACKAGE: String = "com.google.android.gms"

        private val WIDGET_GRACE_PERIOD: Duration = 300.milliseconds

        /**
         * The font families offered when customizing the widget text, as named in the Google Fonts
         * catalog.
         */
        val FONT_FAMILIES: List<String> = listOf(
            // Common
            "Space Grotesk",
            "Playfair Display",

            // Blocky
            "Cinzel Decorative",
            "Italiana",
            "Bebas Neue",
            "Unica One",
            "Dongle",
            "Sue Ellen Francisco",
            "Caveat",
            "Oregano",
            "Cherry Bomb One",
            "Chewy",
            "Special Elite",
            "Limelight",
            "Permanent Marker",
            "Henny Penny",
            "Sixtyfour",

            // Cursive
            "Parisienne",
            "Rouge Script",
            "Tangerine",
            "Dancing Script",
            "Damion",
            "Pacifico",
            "Lobster",
        )
    }
}
