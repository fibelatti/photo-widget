package com.fibelatti.ui.text

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Left,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minTextSize: TextUnit = 10.sp,
    style: TextStyle = LocalTextStyle.current,
) {
    var scaledTextSize: TextUnit by remember { mutableStateOf(style.fontSize) }
    var readyToDraw: Boolean by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            val newTextSize = scaledTextSize * 0.9
            val didOverflow = textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight

            when {
                didOverflow && newTextSize >= minTextSize -> {
                    scaledTextSize = newTextSize
                }

                newTextSize < minTextSize -> {
                    scaledTextSize = minTextSize
                    readyToDraw = true
                }

                else -> readyToDraw = true
            }
        },
        style = style.copy(fontSize = scaledTextSize),
    )
}
