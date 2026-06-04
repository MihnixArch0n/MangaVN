package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import timber.log.Timber

/**
 * Horizontal page-by-page reader content using [HorizontalPager].
 *
 * Handles both LTR and RTL reading modes by providing the appropriate
 * [LayoutDirection] to the composition, which causes [HorizontalPager]
 * to natively reverse its swipe direction.
 *
 * Preloads 2 pages ahead/behind via [beyondViewportPageCount] for smoother scrolling.
 *
 * @param pages The list of image URLs for each page.
 * @param pagerState The [PagerState] controlling the pager position and animations.
 * @param readingMode The current [ReadingMode], used to determine layout direction.
 * @param onEvent Callback invoked when reader content emits UI events.
 * @param modifier Modifier applied to the outer pager container.
 */
@Composable
fun HorizontalReaderContent(
    pages: List<String>,
    pagerState: PagerState,
    readingMode: ReadingMode,
    onEvent: (ReaderEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val navigationCoordinator = remember(pagerState, scope) {
        HorizontalPagerNavigationCoordinator(
            scope = scope,
            currentPage = { pagerState.currentPage },
            lastPageIndex = { pagerState.pageCount - 1 },
            animateToPage = { nextPage, pendingTargetPage, isRetargeted ->
                val durationMillis = horizontalPageAnimationDurationMillis(
                    currentPage = pagerState.currentPage,
                    nextPage = pendingTargetPage,
                    isRetargeted = isRetargeted
                )
                Timber.d(
                    "Reader pager animateScrollToPage: current=%d settled=%d target=%d next=%d duration=%d retargeted=%s",
                    pagerState.currentPage,
                    pagerState.settledPage,
                    pagerState.targetPage,
                    nextPage,
                    durationMillis,
                    isRetargeted
                )
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = durationMillis)
                )
            }
        )
    }
    // RTL mode: flip layout direction so HorizontalPager swipes right-to-left for "next"
    val layoutDirection = when (readingMode) {
        ReadingMode.RTL -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
    val onConfirmedEdgeTap: (Float, Float) -> Unit = { x, width ->
        when (val action = evaluateHorizontalTap(x, width, readingMode)) {
            ReaderTapAction.NEXT_PAGE,
            ReaderTapAction.PREVIOUS_PAGE -> {
                Timber.d(
                    "Reader pager confirmed edge tap: action=%s current=%d settled=%d target=%d",
                    action,
                    pagerState.currentPage,
                    pagerState.settledPage,
                    pagerState.targetPage
                )
                navigationCoordinator.enqueue(action)
            }
            ReaderTapAction.TOGGLE_OVERLAY,
            ReaderTapAction.NONE -> Unit
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.observeConfirmedEdgeTaps(
                viewConfiguration = viewConfiguration,
                onConfirmedEdgeTap = { offset ->
                    onConfirmedEdgeTap(offset.x, pagerState.layoutInfo.viewportSize.width.toFloat())
                },
                onManualDrag = navigationCoordinator::cancelPendingNavigation
            ),
            // Giữ 1 trang trước/sau (không phải 2) để giảm số bitmap thường trú → tránh OOM máy RAM thấp
            beyondViewportPageCount = 1,
            key = { index -> pages.getOrNull(index) ?: "missing-page-$index" }
        ) { pageIndex ->
            pages.getOrNull(pageIndex)?.let { pageUrl ->
                MangaPageItem(
                    imageUrl = pageUrl,
                    index = pageIndex,
                    onConfirmedTap = { x, _, width, _ ->
                        if (evaluateHorizontalTap(x, width, readingMode) == ReaderTapAction.TOGGLE_OVERLAY) {
                            onEvent(ReaderEvent.ToggleOverlay)
                        }
                    },
                    onLongPress = { url, index ->
                        onEvent(ReaderEvent.PageLongPressed(url, index))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private val PreviewHorizontalPages = listOf(
    "https://example.com/reader/page-1.jpg",
    "https://example.com/reader/page-2.jpg",
    "https://example.com/reader/page-3.jpg"
)

@Preview(name = "Horizontal Reader - LTR", showBackground = true)
@Composable
private fun HorizontalReaderContentLtrPreview() {
    MyBooksLibraryTheme {
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { PreviewHorizontalPages.size }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalReaderContent(
                pages = PreviewHorizontalPages,
                pagerState = pagerState,
                readingMode = ReadingMode.LTR,
                onEvent = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Horizontal Reader - RTL", showBackground = true)
@Composable
private fun HorizontalReaderContentRtlPreview() {
    MyBooksLibraryTheme {
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { PreviewHorizontalPages.size }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalReaderContent(
                pages = PreviewHorizontalPages,
                pagerState = pagerState,
                readingMode = ReadingMode.RTL,
                onEvent = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
