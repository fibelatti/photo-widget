package com.fibelatti.ui.foundation

import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.fibelatti.ui.foundation.TextWithLinks.findLinks
import java.util.regex.Pattern

@Composable
fun TextWithLinks(
    text: String,
    modifier: Modifier = Modifier,
    pattern: Pattern = TextWithLinks.urlPattern,
    color: Color = Color.Unspecified,
    linkColor: Color = Color.Blue,
    linkTextDecoration: TextDecoration? = TextDecoration.Underline,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    TextWithLinks(
        text = SpannableString(text),
        modifier = modifier,
        pattern = pattern,
        color = color,
        linkColor = linkColor,
        linkTextDecoration = linkTextDecoration,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}

@Composable
fun TextWithLinks(
    text: Spanned,
    modifier: Modifier = Modifier,
    pattern: Pattern = TextWithLinks.urlPattern,
    color: Color = Color.Unspecified,
    linkColor: Color = Color.Blue,
    linkTextDecoration: TextDecoration? = TextDecoration.Underline,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotatedString = remember(text) {
        buildAnnotatedString {
            append(text)

            val explicitLinks = findLinks(text.toString(), pattern)
            val aTagLinks = text.getSpans(0, text.length, URLSpan::class.java)

            explicitLinks.forEach { linkInfo ->
                addUrlStringAnnotation(
                    url = linkInfo.url,
                    start = linkInfo.start,
                    end = linkInfo.end,
                    linkColor = linkColor,
                    linkTextDecoration = linkTextDecoration,
                )
            }
            aTagLinks.forEach { urlSpan ->
                addUrlStringAnnotation(
                    url = urlSpan.url,
                    start = text.getSpanStart(urlSpan),
                    end = text.getSpanEnd(urlSpan),
                    linkColor = linkColor,
                    linkTextDecoration = linkTextDecoration,
                )
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.pointerInput(text.hashCode()) {
            detectTapGestures { offsetPosition ->
                layoutResult.value?.let {
                    val position = it.getOffsetForPosition(offsetPosition)
                    annotatedString.getStringAnnotations(position, position).firstOrNull()?.let { result ->
                        if (result.tag == "URL") {
                            uriHandler.openUri(result.item)
                        }
                    }
                }
            }
        },
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = { layoutResult.value = it },
        style = style,
    )
}

private fun AnnotatedString.Builder.addUrlStringAnnotation(
    url: String,
    start: Int,
    end: Int,
    linkColor: Color,
    linkTextDecoration: TextDecoration?,
) {
    addStyle(
        style = SpanStyle(color = linkColor, textDecoration = linkTextDecoration),
        start = start,
        end = end,
    )
    addStringAnnotation(
        tag = "URL",
        annotation = url,
        start = start,
        end = end,
    )
}

object TextWithLinks {

    internal val urlPattern: Pattern = Pattern.compile(
        "(?:^|\\W)((ht|f)tp(s?)://|www\\.)" +
            "(([\\w\\-]+\\.)+([\\w\\-.~]+/?)*" +
            "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL,
    )

    internal fun findLinks(
        text: String,
        pattern: Pattern = urlPattern,
    ): List<LinkInfo> {
        val matcher = pattern.matcher(text)
        var matchStart: Int
        var matchEnd: Int

        return buildList {
            while (matcher.find()) {
                matchStart = matcher.start(1)
                matchEnd = matcher.end()

                var url = text.substring(matchStart, matchEnd)
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"

                add(LinkInfo(url, matchStart, matchEnd))
            }
        }
    }

    internal data class LinkInfo(
        val url: String,
        val start: Int,
        val end: Int,
    )
}
