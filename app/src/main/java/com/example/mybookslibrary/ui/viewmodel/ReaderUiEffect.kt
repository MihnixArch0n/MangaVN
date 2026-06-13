package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.domain.model.ReadingMode

sealed interface ReaderUiEffect {
    data class ReadingModeChanged(
        val mode: ReadingMode,
    ) : ReaderUiEffect

    data class NavigateToPage(
        val pageIndex: Int,
    ) : ReaderUiEffect

    data class QuickSavePage(
        val target: ReaderPageActionTarget,
        val fileName: String,
    ) : ReaderUiEffect

    data class SavePageAs(
        val target: ReaderPageActionTarget,
        val fileName: String,
        val extension: String,
    ) : ReaderUiEffect

    data class SharePage(
        val target: ReaderPageActionTarget,
        val fileName: String,
    ) : ReaderUiEffect

    data class ShowPageActionResult(
        val action: ReaderPageAction,
        val errorMessage: String? = null,
    ) : ReaderUiEffect

    data class NavigateToChapter(
        val mangaId: String,
        val chapterId: String,
        val chapterTitle: String,
    ) : ReaderUiEffect

    // Cảnh báo nhảy số chương (PR-4b): thiếu các chương [gapStart..gapEnd] trong nguồn.
    // Text resolve ở layer Compose (appString) để tôn trọng in-app locale.
    data class ShowGapWarning(
        val gapStart: Int,
        val gapEnd: Int,
    ) : ReaderUiEffect
}
