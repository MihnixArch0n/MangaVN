@file:Suppress(
    "CyclomaticComplexMethod",
    "LongParameterList",
    "MaxLineLength",
    "ktlint:standard:function-naming",
)

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme
import com.example.mybookslibrary.ui.viewmodel.ReaderEvent
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Horizontal page-by-page reader content using [HorizontalPager].
 *
 * Handles both LTR and RTL reading modes using [HorizontalPager]'s explicit reverse layout flag.
 * The composition remains LTR so physical tap and drag coordinates stay stable.
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
    modifier: Modifier = Modifier,
    nextChapterTitle: String? = null,
) {
    val navigationController = rememberHorizontalNavigationController(pagerState)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        HorizontalReaderPager(
            pages = pages,
            pagerState = pagerState,
            readingMode = readingMode,
            navigationController = navigationController,
            onEvent = onEvent,
            nextChapterTitle = nextChapterTitle,
            modifier = modifier,
        )
    }
}

@Composable
private fun rememberHorizontalNavigationController(pagerState: PagerState): HorizontalNavigationController {
    val scope = rememberCoroutineScope()
    var isNavigationActive by remember { mutableStateOf(false) }
    val latestNavigationActive = rememberUpdatedState(isNavigationActive)
    val navigationActiveRef = remember { AtomicBoolean(false) }
    val navigationCoordinator =
        remember(pagerState, scope) {
            HorizontalPagerNavigationCoordinator(
                scope = scope,
                settledPage = { pagerState.settledPage },
                lastPageIndex = { pagerState.pageCount - 1 },
                animateToPage = { nextPage, pendingTargetPage, isQueuedNavigation ->
                    val durationMillis =
                        horizontalPageAnimationDurationMillis(
                            currentPage = pagerState.settledPage,
                            nextPage = pendingTargetPage,
                            isQueuedNavigation = isQueuedNavigation,
                        )
                    Timber.v(
                        "Reader pager animateScrollToPage: current=%d settled=%d target=%d next=%d duration=%d queued=%s",
                        pagerState.currentPage,
                        pagerState.settledPage,
                        pagerState.targetPage,
                        nextPage,
                        durationMillis,
                        isQueuedNavigation,
                    )
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = durationMillis),
                    )
                },
                onNavigationActiveChanged = { active ->
                    Timber.v(
                        "Reader pager navigation active changed: active=%s userScrollEnabled=%s current=%d settled=%d target=%d",
                        active,
                        !active,
                        pagerState.currentPage,
                        pagerState.settledPage,
                        pagerState.targetPage,
                    )
                    isNavigationActive = active
                    navigationActiveRef.set(active)
                },
            )
        }

    return remember(navigationCoordinator, latestNavigationActive, navigationActiveRef) {
        HorizontalNavigationController(
            coordinator = navigationCoordinator,
            isActive = {
                navigationActiveRef.get() || latestNavigationActive.value
            },
        )
    }
}

@Composable
private fun HorizontalReaderPager(
    pages: List<String>,
    pagerState: PagerState,
    readingMode: ReadingMode,
    navigationController: HorizontalNavigationController,
    onEvent: (ReaderEvent) -> Unit,
    modifier: Modifier = Modifier,
    nextChapterTitle: String? = null,
) {
    val viewConfiguration = LocalViewConfiguration.current
    val latestReadingMode = rememberUpdatedState(readingMode)
    HorizontalPager(
        state = pagerState,
        modifier =
            modifier.consumeNavigationDuringAnimation(
                viewConfiguration = viewConfiguration,
                isNavigationActive = navigationController.isActive,
                isNavigationTap = { offset ->
                    val width =
                        pagerState.layoutInfo.viewportSize.width
                            .toFloat()
                    evaluateHorizontalTap(offset.x, width, latestReadingMode.value).isPageNavigation()
                },
                onNavigationTap = { offset ->
                    queueShieldedNavigationTap(
                        offsetX = offset.x,
                        width =
                            pagerState.layoutInfo.viewportSize.width
                                .toFloat(),
                        readingMode = latestReadingMode.value,
                        pagerState = pagerState,
                        navigationController = navigationController,
                    )
                },
            ),
        // Giữ 1 trang trước/sau (không phải 2) để giảm số bitmap thường trú, tránh OOM máy RAM thấp.
        beyondViewportPageCount = 1,
        reverseLayout = readingMode == ReadingMode.RTL,
        userScrollEnabled = !navigationController.isActive(),
        key = { index -> pages.getOrNull(index) ?: TRANSITION_PAGE_KEY },
    ) { pageIndex ->
        val pageUrl = pages.getOrNull(pageIndex)
        if (pageUrl == null) {
            // Trang ảo cuối: chuyển tiếp chương (Phase 4 PR-2a). Đặt layout LTR cho nội dung.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ReaderTransitionView(
                    nextChapterTitle = nextChapterTitle,
                    onNextClick = { onEvent(ReaderEvent.NavigateNextChapter) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            // Only the pager should be RTL. Page gestures must keep physical left/right coordinates
            // so Telephoto taps and the pager animation shield evaluate the same tap zone.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MangaPageItem(
                    imageUrl = pageUrl,
                    index = pageIndex,
                    onConfirmedTap = { x, _, width, _ ->
                        handleConfirmedPageTap(
                            x = x,
                            width = width,
                            readingMode = latestReadingMode.value,
                            pagerState = pagerState,
                            navigationController = navigationController,
                            onEvent = onEvent,
                        )
                    },
                    onLongPress = { url, index ->
                        onEvent(ReaderEvent.PageLongPressed(url, index))
                    },
                    allowParentPageSwipe = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private const val TRANSITION_PAGE_KEY = "reader-transition-page"

private fun queueShieldedNavigationTap(
    offsetX: Float,
    width: Float,
    readingMode: ReadingMode,
    pagerState: PagerState,
    navigationController: HorizontalNavigationController,
) {
    when (val action = evaluateHorizontalTap(offsetX, width, readingMode)) {
        ReaderTapAction.NEXT_PAGE,
        ReaderTapAction.PREVIOUS_PAGE,
        -> {
            Timber.v(
                "Reader pager animation shield queued tap: action=%s mode=%s current=%d settled=%d target=%d width=%.1f x=%.1f",
                action,
                readingMode,
                pagerState.currentPage,
                pagerState.settledPage,
                pagerState.targetPage,
                width,
                offsetX,
            )
            navigationController.coordinator.enqueue(action)
        }
        ReaderTapAction.TOGGLE_OVERLAY,
        ReaderTapAction.NONE,
        -> Unit
    }
}

private fun handleConfirmedPageTap(
    x: Float,
    width: Float,
    readingMode: ReadingMode,
    pagerState: PagerState,
    navigationController: HorizontalNavigationController,
    onEvent: (ReaderEvent) -> Unit,
) {
    when (val action = evaluateHorizontalTap(x, width, readingMode)) {
        ReaderTapAction.NEXT_PAGE,
        ReaderTapAction.PREVIOUS_PAGE,
        -> {
            Timber.v(
                "Reader pager confirmed page tap: action=%s mode=%s current=%d settled=%d target=%d active=%s width=%.1f x=%.1f",
                action,
                readingMode,
                pagerState.currentPage,
                pagerState.settledPage,
                pagerState.targetPage,
                navigationController.isActive(),
                width,
                x,
            )
            if (!navigationController.isActive()) {
                navigationController.coordinator.enqueue(action)
            }
        }
        ReaderTapAction.TOGGLE_OVERLAY -> {
            if (!navigationController.isActive()) {
                onEvent(ReaderEvent.ToggleOverlay)
            }
        }
        ReaderTapAction.NONE -> Unit
    }
}

private fun ReaderTapAction.isPageNavigation(): Boolean =
    this == ReaderTapAction.NEXT_PAGE || this == ReaderTapAction.PREVIOUS_PAGE

private data class HorizontalNavigationController(
    val coordinator: HorizontalPagerNavigationCoordinator,
    val isActive: () -> Boolean,
)

private val PreviewHorizontalPages =
    listOf(
        "https://example.com/reader/page-1.jpg",
        "https://example.com/reader/page-2.jpg",
        "https://example.com/reader/page-3.jpg",
    )

@Preview(name = "Horizontal Reader - LTR", showBackground = true)
@Composable
private fun HorizontalReaderContentLtrPreview() {
    MyBooksLibraryTheme {
        val pagerState =
            rememberPagerState(
                initialPage = 1,
                pageCount = { PreviewHorizontalPages.size },
            )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            HorizontalReaderContent(
                pages = PreviewHorizontalPages,
                pagerState = pagerState,
                readingMode = ReadingMode.LTR,
                onEvent = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Horizontal Reader - RTL", showBackground = true)
@Composable
private fun HorizontalReaderContentRtlPreview() {
    MyBooksLibraryTheme {
        val pagerState =
            rememberPagerState(
                initialPage = 1,
                pageCount = { PreviewHorizontalPages.size },
            )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
        ) {
            HorizontalReaderContent(
                pages = PreviewHorizontalPages,
                pagerState = pagerState,
                readingMode = ReadingMode.RTL,
                onEvent = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
