package com.example.mybookslibrary.ui.util

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import kotlin.math.max
import kotlin.math.min

/**
 * Returns the index of the active vertical reader page.
 *
 * Heuristic:
 * - scan visible items only,
 * - compute the visible intersection height for each item,
 * - choose the item with the largest visible area,
 * - prefer the last item when its bottom edge already entered the viewport.
 */
fun LazyListLayoutInfo.findActivePageIndex(): Int {
    val items = visibleItemsInfo
    if (items.isEmpty() || totalItemsCount <= 0) return -1

    val lastIndex = totalItemsCount - 1
    val viewportStart = viewportStartOffset
    val viewportEnd = viewportEndOffset

    val lastItem = items.lastOrNull { it.index == lastIndex }
    if (lastItem != null) {
        val lastItemBottom = lastItem.offset + lastItem.size
        if (lastItemBottom <= viewportEnd) return lastIndex
    }

    var bestIndex = -1
    var bestVisible = -1
    for (item in items) {
        val itemStart = item.offset
        val itemEnd = item.offset + item.size
        val visibleStart = max(itemStart, viewportStart)
        val visibleEnd = min(itemEnd, viewportEnd)
        val visibleSize = (visibleEnd - visibleStart).coerceAtLeast(0)
        if (visibleSize > bestVisible) {
            bestVisible = visibleSize
            bestIndex = item.index
        }
    }
    return bestIndex
}

