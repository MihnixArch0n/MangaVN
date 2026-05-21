package com.example.mybookslibrary.ui.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class LazyListLayoutInfoExtTest {

    @Test
    fun `returns -1 when there are no visible items`() {
        val layoutInfo = TestLayoutInfo(
            visibleItemsInfo = emptyList(),
            totalItemsCount = 0
        )

        assertEquals(-1, layoutInfo.findActivePageIndex())
    }

    @Test
    fun `returns item with largest visible height`() {
        val layoutInfo = TestLayoutInfo(
            visibleItemsInfo = listOf(
                TestItemInfo(index = 0, offset = -400, size = 600), // visible = 200
                TestItemInfo(index = 1, offset = 200, size = 600),  // visible = 600
                TestItemInfo(index = 2, offset = 900, size = 400)   // visible = 100
            ),
            totalItemsCount = 3,
            viewportStartOffset = 0,
            viewportEndOffset = 1000
        )

        assertEquals(1, layoutInfo.findActivePageIndex())
    }

    @Test
    fun `prioritizes last item when its bottom is within viewport`() {
        val layoutInfo = TestLayoutInfo(
            visibleItemsInfo = listOf(
                TestItemInfo(index = 0, offset = 0, size = 600),    // visible = 600
                TestItemInfo(index = 2, offset = 700, size = 200)   // bottom = 900 (inside)
            ),
            totalItemsCount = 3,
            viewportStartOffset = 0,
            viewportEndOffset = 1000
        )

        assertEquals(2, layoutInfo.findActivePageIndex())
    }

    @Test
    fun `returns last item when its bottom is within viewport even if sizes tie`() {
        val layoutInfo = TestLayoutInfo(
            visibleItemsInfo = listOf(
                TestItemInfo(index = 0, offset = 0, size = 500),
                TestItemInfo(index = 1, offset = 500, size = 500)
            ),
            totalItemsCount = 2,
            viewportStartOffset = 0,
            viewportEndOffset = 1000
        )

        assertEquals(1, layoutInfo.findActivePageIndex())
    }

    private data class TestItemInfo(
        override val index: Int,
        override val offset: Int,
        override val size: Int,
        override val key: Any = index,
        override val contentType: Any? = null
    ) : LazyListItemInfo

    private data class TestLayoutInfo(
        override val visibleItemsInfo: List<LazyListItemInfo>,
        override val totalItemsCount: Int,
        override val viewportStartOffset: Int = 0,
        override val viewportEndOffset: Int = 0,
        override val reverseLayout: Boolean = false,
        override val orientation: Orientation = Orientation.Vertical,
        override val beforeContentPadding: Int = 0,
        override val afterContentPadding: Int = 0,
        override val viewportSize: IntSize = IntSize(0, 0)
    ) : LazyListLayoutInfo
}
