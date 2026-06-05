@file:Suppress("ktlint:standard:function-naming")

package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.ui.theme.MyBooksLibraryTheme

private val PreviewReaderPages =
    listOf(
        "https://example.com/reader/page-1.jpg",
        "https://example.com/reader/page-2.jpg",
        "https://example.com/reader/page-3.jpg",
    )

@Preview(name = "Reader - Horizontal", showBackground = true)
@Composable
private fun ReaderHorizontalPreview() {
    MyBooksLibraryTheme {
        ReaderPreviewLayout(
            chapterTitle = "Chapter 12: Lost Pages",
            pages = PreviewReaderPages,
            currentPage = 1,
            readingMode = ReadingMode.LTR,
        )
    }
}

@Preview(name = "Reader - Vertical", showBackground = true)
@Composable
private fun ReaderVerticalPreview() {
    MyBooksLibraryTheme {
        ReaderPreviewLayout(
            chapterTitle = "Chapter 12: Lost Pages",
            pages = PreviewReaderPages,
            currentPage = 1,
            readingMode = ReadingMode.VERTICAL,
        )
    }
}

@Composable
private fun ReaderPreviewLayout(
    chapterTitle: String,
    pages: List<String>,
    currentPage: Int,
    readingMode: ReadingMode,
) {
    val listState = rememberLazyListState()
    val pagerState =
        rememberPagerState(
            initialPage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0)),
            pageCount = { pages.size },
        )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        when (readingMode) {
            ReadingMode.VERTICAL -> {
                VerticalReaderContent(
                    pages = pages,
                    listState = listState,
                    onEvent = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ReadingMode.LTR, ReadingMode.RTL -> {
                HorizontalReaderContent(
                    pages = pages,
                    pagerState = pagerState,
                    readingMode = readingMode,
                    onEvent = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ReaderTopBar(
            chapterTitle = chapterTitle,
            isVisible = true,
            onBackClick = { },
        )
        ReaderBottomBar(
            isVisible = true,
            state =
                ReaderBottomBarState(
                    currentPage = currentPage,
                    totalPages = pages.size,
                    currentReadingMode = readingMode,
                ),
            onToggleReadingMode = { },
        )
    }
}
