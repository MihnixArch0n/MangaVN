package com.example.mybookslibrary.domain.model

/**
 * Represents the action triggered by a user tap on the reader screen.
 *
 * The action is determined by the tap position relative to the screen width
 * and the current [ReadingMode].
 *
 * - [NEXT_PAGE]: Navigate to the next page.
 * - [PREVIOUS_PAGE]: Navigate to the previous page.
 * - [TOGGLE_OVERLAY]: Show or hide the reader overlay (top/bottom bars).
 * - [NONE]: No action (used for edge cases like invalid coordinates).
 */
enum class ReaderTapAction {
    NEXT_PAGE,
    PREVIOUS_PAGE,
    TOGGLE_OVERLAY,
    NONE
}
