package com.example.mybookslibrary.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

@Serializable
data object Login

@Serializable
data object Register

@Serializable
data object Discover

@Serializable
data object Search

@Serializable
data object Library

@Serializable
data object Setting

@Serializable
data object ReadingHistory

@Serializable
data object Profile

@Serializable
data object Statistics

@Serializable
data object Downloads

@Serializable
data object EditProfile

@Serializable
data object ChangePassword

@Serializable
data class MangaDetail(
    val mangaId: String,
)

@Serializable
data class MangaReview(
    val mangaId: String,
)

@Serializable
data class Reader(
    val mangaId: String,
    val chapterId: String,
    val chapterTitle: String,
    val startPageIndex: Int,
)
