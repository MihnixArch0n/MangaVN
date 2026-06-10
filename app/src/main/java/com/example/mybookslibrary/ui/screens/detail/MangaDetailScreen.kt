package com.example.mybookslibrary.ui.screens.detail

import androidx.compose.runtime.Composable

@Composable
fun MangaDetailScreen(
    mangaId: String,
    onBackClick: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String, chapterTitle: String, startPageIndex: Int) -> Unit,
    onReviewClick: (mangaId: String) -> Unit = {},
    onShareClick: ((mangaTitle: String) -> Unit)? = null,
) {
    com.example.mybookslibrary.ui.screens.MangaDetailScreen(
        mangaId = mangaId,
        onBackClick = onBackClick,
        onReadChapter = onReadChapter,
        onReviewClick = onReviewClick,
        onShareClick = onShareClick,
    )
}
