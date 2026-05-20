package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

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
 * @param onPageLongPress Callback invoked on long-press, passing the page's image URL and index.
 * @param modifier Modifier applied to the outer pager container.
 */
@Composable
fun HorizontalReaderContent(
    pages: List<String>,
    pagerState: PagerState,
    readingMode: ReadingMode,
    onPageLongPress: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // RTL mode: flip layout direction so HorizontalPager swipes right-to-left for "next"
    val layoutDirection = when (readingMode) {
        ReadingMode.RTL -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            beyondViewportPageCount = 2,
            key = { index -> pages[index] }
        ) { pageIndex ->
            MangaPageItem(
                imageUrl = pages[pageIndex],
                index = pageIndex,
                onLongPress = onPageLongPress,
                modifier = Modifier.fillMaxSize()
            )
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
                onPageLongPress = { _, _ -> },
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
                onPageLongPress = { _, _ -> },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
