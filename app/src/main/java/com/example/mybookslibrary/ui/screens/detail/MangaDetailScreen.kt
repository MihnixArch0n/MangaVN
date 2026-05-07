package com.example.mybookslibrary.ui.screens.detail

import androidx.compose.runtime.Composable

@Composable
fun MangaDetailScreen(
    mangaId: String,
    onOpenReader: (mangaId: String, chapterId: String, chapterTitle: String, startPageIndex: Int) -> Unit
) {
    com.example.mybookslibrary.ui.screens.MangaDetailScreen(
        mangaId = mangaId,
        title = "",
        coverArt = "",
        description = "",
        tags = emptyList(),
        onBackClick = {},
        onReadChapter = { id, chapterId, chapterTitle ->
            onOpenReader(id, chapterId, chapterTitle, 0)
        }
    )
}



