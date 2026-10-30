package com.fibelatti.photowidget.platform

import android.graphics.Typeface
import android.util.LruCache
import com.fibelatti.photowidget.model.PhotoWidgetTextFont
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Loads the typeface of a [PhotoWidgetTextFont]: fonts from the Google Fonts catalog through
 * [GoogleFontsLoader], and custom fonts from their file.
 */
@Singleton
class TextFontLoader @Inject constructor(
    private val googleFontsLoader: GoogleFontsLoader,
) {

    /**
     * Custom fonts already loaded, keyed by path. Choosing a file with the same name as an existing
     * one replaces it, so each entry keeps the modification time of the file it was loaded from.
     *
     * Every font picked while configuring a widget is staged under a new path, so the cache is
     * bounded to keep the fonts that were only tried out from piling up.
     */
    private val customFontCache: LruCache<String, CustomFontEntry> = LruCache(MAX_CACHED_CUSTOM_FONTS)

    /**
     * The typeface of [font] if it has already been loaded, or `null` otherwise. Doesn't access the
     * disk, so it can be called from the main thread.
     */
    fun peekTypeface(font: PhotoWidgetTextFont): Typeface? {
        return when (font) {
            is PhotoWidgetTextFont.Default -> Typeface.DEFAULT
            is PhotoWidgetTextFont.GoogleFont -> googleFontsLoader.peekTypeface(fontFamily = font.family)
            is PhotoWidgetTextFont.Custom -> customFontCache[font.path]?.typeface
        }
    }

    /**
     * The typeface of [font] used to render the text, or [Typeface.DEFAULT] when it fails to load.
     * Waits for it to load, so widgets use [getWidgetTypeface] instead.
     */
    suspend fun getTypeface(font: PhotoWidgetTextFont): Typeface {
        return when (font) {
            is PhotoWidgetTextFont.Default -> Typeface.DEFAULT
            is PhotoWidgetTextFont.GoogleFont -> googleFontsLoader.getTypeface(fontFamily = font.family)
            is PhotoWidgetTextFont.Custom -> loadCustomTypeface(file = File(font.path)) ?: Typeface.DEFAULT
        }
    }

    /**
     * The typeface of [font] used to render a widget, or [Typeface.DEFAULT] when it fails to load.
     * See [GoogleFontsLoader.getWidgetTypeface] for when [onLateLoad] is invoked.
     */
    suspend fun getWidgetTypeface(font: PhotoWidgetTextFont, onLateLoad: suspend () -> Unit): Typeface {
        return when (font) {
            is PhotoWidgetTextFont.GoogleFont -> googleFontsLoader.getWidgetTypeface(
                fontFamily = font.family,
                onLateLoad = onLateLoad,
            )

            is PhotoWidgetTextFont.Default, is PhotoWidgetTextFont.Custom -> getTypeface(font = font)
        }
    }

    private suspend fun loadCustomTypeface(file: File): Typeface? = withContext(Dispatchers.IO) {
        val lastModified: Long = file.lastModified()

        customFontCache[file.path]
            ?.takeIf { it.lastModified == lastModified }
            ?.let { return@withContext it.typeface }

        val typeface: Typeface? = createTypeface(file = file)

        if (typeface == null) {
            Timber.w("Failed to load custom font %s", mapOf("path" to file.path))
        } else {
            customFontCache.put(file.path, CustomFontEntry(typeface = typeface, lastModified = lastModified))
        }

        typeface
    }

    private data class CustomFontEntry(val typeface: Typeface, val lastModified: Long)

    companion object {

        /** Fonts evicted beyond this are loaded from their file again the next time they are used. */
        private const val MAX_CACHED_CUSTOM_FONTS: Int = 10

        /** TrueType, OpenType with CFF outlines, Apple TrueType, and font collections. */
        private val FONT_SIGNATURES: List<ByteArray> = listOf(
            byteArrayOf(0x00, 0x01, 0x00, 0x00),
            "OTTO".toByteArray(Charsets.US_ASCII),
            "true".toByteArray(Charsets.US_ASCII),
            "ttcf".toByteArray(Charsets.US_ASCII),
        )

        /**
         * The typeface in [file], or `null` when it isn't a font file the system can load.
         */
        fun createTypeface(file: File): Typeface? = Typeface.Builder(file).build()

        /**
         * Whether [input] starts with the signature of a font format. Lets a file be rejected
         * before it is copied, while [createTypeface] remains the check that it can be loaded.
         */
        fun hasFontSignature(input: InputStream): Boolean {
            val signature = ByteArray(size = FONT_SIGNATURES.first().size)

            try {
                DataInputStream(input).readFully(signature)
            } catch (_: EOFException) {
                return false
            }

            return FONT_SIGNATURES.any { it.contentEquals(signature) }
        }
    }
}
