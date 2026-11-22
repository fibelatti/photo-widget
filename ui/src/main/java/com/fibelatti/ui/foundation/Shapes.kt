package com.fibelatti.ui.foundation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Shapes {

    val GroupCornerRadius: Dp = 16.dp
    val SeamCornerRadius: Dp = 4.dp

    private val mediumCornerSize: CornerSize = CornerSize(GroupCornerRadius)
    private val smallCornerSize: CornerSize = CornerSize(SeamCornerRadius)

    val TopShape: Shape = RoundedCornerShape(
        topStart = mediumCornerSize,
        topEnd = mediumCornerSize,
        bottomStart = smallCornerSize,
        bottomEnd = smallCornerSize,
    )

    val BottomShape: Shape = RoundedCornerShape(
        topStart = smallCornerSize,
        topEnd = smallCornerSize,
        bottomStart = mediumCornerSize,
        bottomEnd = mediumCornerSize,
    )

    val StartShape: Shape = RoundedCornerShape(
        topStart = mediumCornerSize,
        topEnd = smallCornerSize,
        bottomStart = mediumCornerSize,
        bottomEnd = smallCornerSize,
    )

    val EndShape: Shape = RoundedCornerShape(
        topStart = smallCornerSize,
        topEnd = mediumCornerSize,
        bottomStart = smallCornerSize,
        bottomEnd = mediumCornerSize,
    )

    val MiddleShape: Shape = RoundedCornerShape(smallCornerSize)

    val StandaloneShape: Shape = RoundedCornerShape(mediumCornerSize)

    /**
     * The shape of the item at [index] in a vertical group of [count] items: the outer corners of the
     * group are rounded with [GroupCornerRadius], and the seams between items with [SeamCornerRadius].
     */
    fun segment(index: Int, count: Int): Shape = when {
        count == 1 -> StandaloneShape
        index == 0 -> TopShape
        index == count - 1 -> BottomShape
        else -> MiddleShape
    }
}
