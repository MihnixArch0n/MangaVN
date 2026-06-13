package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.domain.model.ReadingMode

data class ReaderState(
    val chapterTitle: String = "",
    val pages: List<String> = emptyList(),
    val isOverlayVisible: Boolean = false,
    val lastReadPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentReadingMode: ReadingMode = ReadingMode.LTR,
    val selectedPageActionTarget: ReaderPageActionTarget? = null,
    val prevChapterId: String? = null,
    val prevChapterTitle: String? = null,
    val nextChapterId: String? = null,
    val nextChapterTitle: String? = null,
    // Reader comfort (Phase 4 PR-1)
    val keepScreenOn: Boolean = false,
    val volumeKeyNav: Boolean = false,
    val brightness: Float = 1.0f,
    val background: ReaderBackground = ReaderBackground.BLACK,
    val isComfortPanelVisible: Boolean = false,
    // Seamless navigation (Phase 4 PR-2a)
    val autoAdvance: Boolean = false,
    // Auto-download chương kế (Phase 4 PR-2b)
    val autoDownloadNext: Boolean = false,
)

data class ReaderPageActionTarget(
    val pageUrl: String,
    val pageIndex: Int,
)
