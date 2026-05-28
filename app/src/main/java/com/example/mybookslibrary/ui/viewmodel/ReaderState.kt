package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.domain.model.ReadingMode

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentReadingMode: ReadingMode = ReadingMode.LTR,
    val selectedPageActionTarget: ReaderPageActionTarget? = null
)

data class ReaderPageActionTarget(
    val pageUrl: String,
    val pageIndex: Int
)
