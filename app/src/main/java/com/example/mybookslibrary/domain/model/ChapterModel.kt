package com.example.mybookslibrary.domain.model

data class ChapterModel(
    val id: String,
    val mangaId: String,
    val volume: String?,
    val chapterNumber: String?,
    val title: String?,
    val pages: Int,
    val isUnavailable: Boolean
)

