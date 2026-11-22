package com.fibelatti.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fibelatti.ui.foundation.Shapes

/**
 * Receiver scope of [SegmentedColumn], used to declare the items of the group.
 */
class SegmentedGroupScope internal constructor() {

    internal val segments: MutableList<SegmentedItem> = mutableListOf()

    /**
     * Adds an item to the group.
     *
     * @param key a stable identifier for the item. Like in a lazy list, it's only needed when items
     * before this one can be added or removed, so that its state moves with it.
     * @param visible whether the item is shown. Changes are animated, and once a hidden item finishes
     * animating out it no longer counts towards the shapes of the other items.
     * @param content the item, which should apply the given `shape` to its container.
     */
    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable (shape: Shape) -> Unit,
    ) {
        segments += SegmentedItem(key = key, visible = visible, content = content)
    }
}

internal class SegmentedItem(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable (Shape) -> Unit,
)

/**
 * A vertical group of items that look connected: the group has rounded outer corners and each item
 * is separated by [ListItemDefaults.SegmentedGap]. Each item receives the shape matching its position
 * among the items currently on screen, which include hidden items until they finish animating out.
 * When that position changes, the shape and the gap above the item animate to their new values.
 */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    content: SegmentedGroupScope.() -> Unit,
) {
    val items: List<SegmentedItem> = SegmentedGroupScope().apply(content).segments
    val visibleStates: List<MutableTransitionState<Boolean>> = items.mapIndexed { index: Int, item: SegmentedItem ->
        key(item.key ?: index) {
            remember { MutableTransitionState(item.visible) }.apply { targetState = item.visible }
        }
    }
    val onScreenCount: Int = visibleStates.count { it.isOnScreen }
    var onScreenBefore = 0

    Column(modifier = modifier) {
        items.forEachIndexed { index: Int, item: SegmentedItem ->
            val visibleState: MutableTransitionState<Boolean> = visibleStates[index]
            val isFirst: Boolean = onScreenBefore == 0
            val isLast: Boolean = onScreenBefore == onScreenCount - 1

            key(item.key ?: index) {
                val gap: Dp by animateDpAsState(
                    targetValue = if (isFirst) 0.dp else ListItemDefaults.SegmentedGap,
                )
                val topCorner: Dp by animateDpAsState(
                    targetValue = if (isFirst) Shapes.GroupCornerRadius else Shapes.SeamCornerRadius,
                )
                val bottomCorner: Dp by animateDpAsState(
                    targetValue = if (isLast) Shapes.GroupCornerRadius else Shapes.SeamCornerRadius,
                )
                val shape: Shape = RoundedCornerShape(
                    topStart = topCorner,
                    topEnd = topCorner,
                    bottomStart = bottomCorner,
                    bottomEnd = bottomCorner,
                )

                AnimatedVisibility(visibleState = visibleState) {
                    Box(modifier = Modifier.padding(top = gap)) {
                        item.content(shape)
                    }
                }
            }

            if (visibleState.isOnScreen) onScreenBefore++
        }
    }
}

/**
 * Whether the item is shown or still animating in or out.
 */
private val MutableTransitionState<Boolean>.isOnScreen: Boolean
    get() = currentState || targetState

/**
 * Adds [items] to a `LazyList` as a segmented group, the lazy counterpart of [SegmentedColumn].
 * Each item receives the shape matching its position in the group. The gap between items is
 * expected to come from the list's `verticalArrangement`.
 */
fun <T> LazyListScope.segmentedItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T, shape: Shape) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = key?.let { itemKey -> { _: Int, item: T -> itemKey(item) } },
        contentType = { _: Int, item: T -> contentType(item) },
    ) { index: Int, item: T ->
        itemContent(item, Shapes.segment(index = index, count = items.size))
    }
}
