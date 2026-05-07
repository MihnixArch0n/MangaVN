package com.example.mybookslibrary.domain.model

data class MangaModel(
    val id: String,
    val title: String,
    val description: String,
    val coverArt: String?,
    val tags: List<String>
)


