package com.fibelatti.photowidget.configure

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Accepts up to 6 hex characters, without the `#` prefix. Edits that would produce anything else
 * are rejected as a whole.
 */
object HexColorInputTransformation : InputTransformation {

    private const val MAX_LENGTH = 6

    override fun TextFieldBuffer.transformInput() {
        if (length > MAX_LENGTH || !asCharSequence().all(::isHexChar)) {
            revertAllChanges()
        }
    }

    private fun isHexChar(char: Char): Boolean = char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'
}
