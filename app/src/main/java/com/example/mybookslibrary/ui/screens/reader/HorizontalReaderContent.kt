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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import java.util.concurrent.atomic.AtomicBoolean
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
    var isNavigationActive by remember { mutableStateOf(false) }
    val latestNavigationActive = rememberUpdatedState(isNavigationActive)
    val navigationActiveRef = remember { AtomicBoolean(false) }
    val navigationCoordinator = remember(pagerState, scope) {
        HorizontalPagerNavigationCoordinator(
            scope = scope,
            currentPage = { pagerState.currentPage },
            lastPageIndex = { pagerState.pageCount - 1 },
            animateToPage = { nextPage, pendingTargetPage, isQueuedNavigation ->
                val durationMillis = horizontalPageAnimationDurationMillis(
                    currentPage = pagerState.currentPage,
                    nextPage = pendingTargetPage,
                    isQueuedNavigation = isQueuedNavigation
                )
                Timber.d(
                    "Reader pager animateScrollToPage: current=%d settled=%d target=%d next=%d duration=%d queued=%s",
                    pagerState.currentPage,
                    pagerState.settledPage,
                    pagerState.targetPage,
                    nextPage,
                    durationMillis,
                    isQueuedNavigation
                )
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = durationMillis)
                )
            },
            onNavigationActiveChanged = { active ->
                Timber.d(
                    "Reader pager navigation active changed: active=%s userScrollEnabled=%s current=%d settled=%d target=%d",
                    active,
                    !active,
                    pagerState.currentPage,
                    pagerState.settledPage,
                    pagerState.targetPage
                )
                isNavigationActive = active
                navigationActiveRef.set(active)
            }
        )
    }
    // RTL mode: flip layout direction so HorizontalPager swipes right-to-left for "next"
    val layoutDirection = when (readingMode) {
        ReadingMode.RTL -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
    val onConfirmedPageTap: (Float, Float) -> Unit = { x, width ->
        when (val action = evaluateHorizontalTap(x, width, readingMode)) {
            ReaderTapAction.NEXT_PAGE,
            ReaderTapAction.PREVIOUS_PAGE -> {
                Timber.d(
                    "Reader pager confirmed page tap: action=%s current=%d settled=%d target=%d active=%s width=%.1f x=%.1f",
                    action,
                    pagerState.currentPage,
                    pagerState.settledPage,
                    pagerState.targetPage,
                    isNavigationActive,
                    width,
                    x
                )
                if (!isNavigationActive) {
                    navigationCoordinator.enqueue(action)
                }
            }
            ReaderTapAction.TOGGLE_OVERLAY -> {
                if (!isNavigationActive) {
                    onEvent(ReaderEvent.ToggleOverlay)
                }
            }
            ReaderTapAction.NONE -> Unit
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.consumeNavigationDuringAnimation(
                viewConfiguration = viewConfiguration,
                isNavigationActive = { navigationActiveRef.get() || latestNavigationActive.value },
                isNavigationTap = { offset ->
                    val width = pagerState.layoutInfo.viewportSize.width.toFloat()
                    when (evaluateHorizontalTap(offset.x, width, readingMode)) {
                        ReaderTapAction.NEXT_PAGE,
                        ReaderTapAction.PREVIOUS_PAGE -> true
                        ReaderTapAction.TOGGLE_OVERLAY,
                        ReaderTapAction.NONE -> false
                    }
                },
                onNavigationTap = { offset ->
                    val width = pagerState.layoutInfo.viewportSize.width.toFloat()
                    when (val action = evaluateHorizontalTap(offset.x, width, readingMode)) {
                        ReaderTapAction.NEXT_PAGE,
                        ReaderTapAction.PREVIOUS_PAGE -> {
                            Timber.d(
                                "Reader pager animation shield queued tap: action=%s current=%d settled=%d target=%d width=%.1f x=%.1f",
                                action,
                                pagerState.currentPage,
                                pagerState.settledPage,
                                pagerState.targetPage,
                                width,
                                offset.x
                            )
                            navigationCoordinator.enqueue(action)
                        }
                        ReaderTapAction.TOGGLE_OVERLAY,
                        ReaderTapAction.NONE -> Unit
                    }
                }
            ),
            // Giữ 1 trang trước/sau (không phải 2) để giảm số bitmap thường trú → tránh OOM máy RAM thấp
            beyondViewportPageCount = 1,
            userScrollEnabled = !isNavigationActive,
            key = { index -> pages.getOrNull(index) ?: "missing-page-$index" }
        ) { pageIndex ->
            pages.getOrNull(pageIndex)?.let { pageUrl ->
                MangaPageItem(
                    imageUrl = pageUrl,
                    index = pageIndex,
                    onConfirmedTap = { x, _, width, _ ->
                        onConfirmedPageTap(x, width)
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
