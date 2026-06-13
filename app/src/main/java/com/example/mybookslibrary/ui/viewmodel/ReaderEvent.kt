package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.domain.model.ReadingMode

sealed interface ReaderEvent {
    data object RetryLoadPages : ReaderEvent
    data object ToggleOverlay : ReaderEvent

    // Reader comfort (Phase 4 PR-1)
    data object ToggleComfortPanel : ReaderEvent

    /** Cập nhật độ sáng tức thời (preview mượt khi kéo slider) — KHÔNG ghi DataStore. */
    data class SetBrightness(
        val value: Float,
    ) : ReaderEvent

    /** Lưu độ sáng hiện tại xuống DataStore (gọi khi thả slider) để tránh ghi mỗi tick. */
    data object CommitBrightness : ReaderEvent

    data class SetBackground(
        val background: ReaderBackground,
    ) : ReaderEvent

    /** Phím âm lượng lật sang trang kế (volume down). */
    data object VolumeKeyNextPage : ReaderEvent

    /** Phím âm lượng lật về trang trước (volume up). */
    data object VolumeKeyPrevPage : ReaderEvent

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

    data object NavigatePrevChapter : ReaderEvent

    data object NavigateNextChapter : ReaderEvent

    // Seamless navigation (Phase 4 PR-2a)
    /** User lướt tới trang chuyển tiếp cuối chương → preload + (nếu autoAdvance) tự sang chương sau. */
    data object ReachedTransitionPage : ReaderEvent

    /** Rời trang chuyển tiếp (lướt ngược lại) → hủy auto-advance đang chờ. */
    data object LeftTransitionPage : ReaderEvent
}

enum class ReaderPageAction {
    QuickSave,
    SaveAs,
    Share,
}
