package com.fibelatti.photowidget.platform

import android.content.Context
import android.net.Uri
import java.io.InputStream
import javax.inject.Inject

class ImageFormatDetector @Inject constructor(
    factory: ImageFormatMatcherFactory,
) {

    private val matchers: List<ImageFormatMatcher> by lazy(factory::createAllMatchers)

    fun getImageFormat(context: Context, imageUri: Uri): ImageFormat {
        return context.contentResolver.openInputStream(imageUri)
            ?.use(::getImageFormat)
            ?: ImageFormat.UNDEFINED
    }

    fun getImageFormat(inputStream: InputStream): ImageFormat {
        val buffer = ByteArray(size = matchers.maxOf { it.size })

        inputStream.read(buffer)

        return matchers.firstOrNull { it.match(input = buffer) }?.format
            ?: ImageFormat.UNDEFINED
    }
}

class ImageFormatMatcherFactory @Inject constructor() {

    fun createAllMatchers(): List<ImageFormatMatcher> = listOf(
        createJpegMatcher(),
        createPngMatcher(),
    )

    fun createJpegMatcher(): ImageFormatMatcher {
        return ImageFormatMatcher(
            format = ImageFormat.JPEG,
            signature = byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
            ),
        )
    }

    fun createPngMatcher(): ImageFormatMatcher {
        return ImageFormatMatcher(
            format = ImageFormat.PNG,
            signature = byteArrayOf(
                0x89.toByte(),
                0x50.toByte(),
                0x4E.toByte(),
                0x47.toByte(),
                0x0D.toByte(),
                0x0A.toByte(),
                0x1A.toByte(),
                0x0A.toByte(),
            ),
        )
    }
}

class ImageFormatMatcher(
    val format: ImageFormat,
    private val signature: ByteArray,
) {

    val size: Int
        get() = signature.size

    fun match(input: ByteArray): Boolean {
        if (size > input.size) {
            return false
        }

        for (i: Int in signature.indices) {
            if (signature[i] != input[i]) {
                return false
            }
        }

        return true
    }
}

enum class ImageFormat {
    JPEG,
    PNG,
    UNDEFINED,
}
