package com.example.mybookslibrary.ui.screens.detail

import androidx.compose.runtime.Composable

@Composable
fun MangaDetailScreen(
    mangaId: String,
    title: String,
    coverArt: String,
    description: String,
    tags: List<String>,
    onBackClick: () -> Unit,
    onReadChapter: (mangaId: String, chapterId: String, chapterTitle: String) -> Unit,
    onReviewClick: (mangaId: String) -> Unit = {}
) {
    com.example.mybookslibrary.ui.screens.MangaDetailScreen(
        mangaId = mangaId,
        title = title,
        coverArt = coverArt,
        description = description,
        tags = tags,
        onBackClick = onBackClick,
        onReadChapter = onReadChapter,
        onReviewClick = onReviewClick
    )
}



