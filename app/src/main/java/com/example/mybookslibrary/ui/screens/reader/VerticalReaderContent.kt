@file:Suppress(
    "LongMethod",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.example.mybookslibrary.ui.util.findActivePageIndex
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import timber.log.Timber

@Composable
internal fun VerticalReaderContent(
    pages: List<String>,
    listState: LazyListState,
    onEvent: (ReaderEvent) -> Unit,
    modifier: Modifier = Modifier,
    selectedPageIndex: Int? = null,
    nextChapterTitle: String? = null,
    onZoomFractionChanged: (Float?) -> Unit = {},
) {
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 3f))
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var containerHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(zoomableState) {
        snapshotFlow { zoomableState.zoomFraction }
            .distinctUntilChanged()
            .collect { zoomFraction ->
                Timber.v("Reader webtoon global zoom changed: zoomFraction=%s", zoomFraction)
                onZoomFractionChanged(zoomFraction)
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged {
                    containerWidthPx = it.width
                    containerHeightPx = it.height
                }.zoomable(
                    state = zoomableState,
                    onClick = { offset ->
                        Timber.v("Reader webtoon container tap: x=%.1f y=%.1f", offset.x, offset.y)
                        onEvent(
                            ReaderEvent.TapOnScreen(
                                x = offset.x,
                                y = offset.y,
                                width = containerWidthPx.toFloat(),
                                height = containerHeightPx.toFloat(),
                            ),
                        )
                    },
                    onLongClick = { offset ->
                        val pageIndex = listState.findPageIndexAtViewportOffset(offset.y)
                        val pageUrl = pages.getOrNull(pageIndex)
                        Timber.v(
                            "Reader webtoon container long-click: x=%.1f y=%.1f page=%d url=%s",
                            offset.x,
                            offset.y,
                            pageIndex + 1,
                            pageUrl,
                        )
                        if (pageUrl != null && pageIndex >= 0) {
                            onEvent(ReaderEvent.PageLongPressed(pageUrl, pageIndex))
                        }
                    },
                ),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(items = pages, key = { _, page -> page }) { index, page ->
                WebtoonPageItem(
                    imageUrl = page,
                    index = index,
                    isSelected = index == selectedPageIndex,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Trang chuyển tiếp cuối chương (Phase 4 PR-2a) — item cuối, cao 1 màn hình.
            item(key = "reader-transition-page") {
                ReaderTransitionView(
                    nextChapterTitle = nextChapterTitle,
                    onNextClick = { onEvent(ReaderEvent.NavigateNextChapter) },
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        }
    }
}

private fun LazyListState.findPageIndexAtViewportOffset(y: Float): Int {
    val item =
        layoutInfo.visibleItemsInfo.firstOrNull { visibleItem ->
            y >= visibleItem.offset && y <= visibleItem.offset + visibleItem.size
        }
    return item?.index ?: layoutInfo.findActivePageIndex()
}
