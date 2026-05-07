package com.example.mybookslibrary.domain.model

enum class ChapterReadingStatus {
    UNREAD,
    READING,
    COMPLETED
}

data class ChapterWithProgressModel(
    val chapterId: String,
    val mangaId: String,
    val volume: String?,
    val chapterNumber: String?,
    val title: String?,
    val status: ChapterReadingStatus,
    val lastReadPage: Int,
    val totalPages: Int
)

