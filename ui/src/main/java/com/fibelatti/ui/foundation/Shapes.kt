package com.fibelatti.ui.foundation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object Shapes {

    private val mediumCornerSize: CornerSize = CornerSize(12.dp)
    private val smallCornerSize: CornerSize = CornerSize(2.dp)

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
}
