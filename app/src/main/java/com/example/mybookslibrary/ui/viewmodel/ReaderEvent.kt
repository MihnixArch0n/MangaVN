package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.domain.model.ReadingMode

sealed interface ReaderEvent {
    data object RetryLoadPages : ReaderEvent
    data object ToggleOverlay : ReaderEvent

    data class ChangeReadingMode(
        val mode: ReadingMode,
    ) : ReaderEvent

    data object CycleReadingMode : ReaderEvent

    data class JumpToPage(
        val pageIndex: Int,
    ) : ReaderEvent

    data class VisiblePageChanged(
        val pageIndex: Int,
    ) : ReaderEvent

    data class FlushProgress(
        val pageIndex: Int?,
    ) : ReaderEvent

    data class TapOnScreen(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    ) : ReaderEvent

    data class PageLongPressed(
        val pageUrl: String,
        val pageIndex: Int,
    ) : ReaderEvent

    data object DismissPageActions : ReaderEvent

    data class PageActionSelected(
        val action: ReaderPageAction,
    ) : ReaderEvent

    data class PageActionCompleted(
        val action: ReaderPageAction,
    ) : ReaderEvent

    data class PageActionFailed(
        val action: ReaderPageAction,
        val message: String,
    ) : ReaderEvent
}

enum class ReaderPageAction {
    QuickSave,
    SaveAs,
    Share,
}
