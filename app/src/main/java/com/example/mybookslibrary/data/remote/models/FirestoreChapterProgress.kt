package com.example.mybookslibrary.data.remote.models

data class FirestoreChapterProgress(
    val chapterId: String = "",
    val mangaId: String = "",
    val status: String = "",
    val lastReadPage: Int = 0,
    val totalPages: Int = 0,
    val updatedAt: Long = 0L
)
