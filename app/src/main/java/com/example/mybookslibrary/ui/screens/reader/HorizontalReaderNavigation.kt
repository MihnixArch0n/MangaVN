package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import kotlin.math.abs

internal fun evaluateHorizontalTap(
    x: Float,
    width: Float,
    readingMode: ReadingMode,
): ReaderTapAction {
    if (!x.isFinite() || !width.isFinite() || width <= 0f) return ReaderTapAction.NONE

    val tapRatio = x.coerceIn(0f, width) / width
    return when {
        tapRatio < LEFT_ZONE_END_RATIO ->
            when (readingMode) {
                ReadingMode.LTR -> ReaderTapAction.PREVIOUS_PAGE
                ReadingMode.RTL -> ReaderTapAction.NEXT_PAGE
                ReadingMode.VERTICAL -> ReaderTapAction.TOGGLE_OVERLAY
            }
        tapRatio >= RIGHT_ZONE_START_RATIO ->
            when (readingMode) {
                ReadingMode.LTR -> ReaderTapAction.NEXT_PAGE
                ReadingMode.RTL -> ReaderTapAction.PREVIOUS_PAGE
                ReadingMode.VERTICAL -> ReaderTapAction.TOGGLE_OVERLAY
            }
        else -> ReaderTapAction.TOGGLE_OVERLAY
    }
}

internal fun calculateHorizontalTargetPage(
    targetPage: Int,
    action: ReaderTapAction,
    lastPageIndex: Int,
): Int? {
    val pageDelta =
        if (lastPageIndex < 0) {
            null
        } else {
            when (action) {
                ReaderTapAction.NEXT_PAGE -> 1
                ReaderTapAction.PREVIOUS_PAGE -> -1
                ReaderTapAction.TOGGLE_OVERLAY,
                ReaderTapAction.NONE,
                -> null
            }
        }
    return pageDelta?.let { (targetPage + it).coerceIn(0, lastPageIndex) }
}

internal fun horizontalPageAnimationDurationMillis(
    currentPage: Int,
    nextPage: Int,
    isQueuedNavigation: Boolean = false,
): Int =
    if (isQueuedNavigation || abs(currentPage - nextPage) > 1) {
        FAST_PAGE_ANIMATION_DURATION_MILLIS
    } else {
        DEFAULT_PAGE_ANIMATION_DURATION_MILLIS
    }

private const val LEFT_ZONE_END_RATIO = 0.25f
private const val RIGHT_ZONE_START_RATIO = 0.75f
internal const val DEFAULT_PAGE_ANIMATION_DURATION_MILLIS = 220
internal const val FAST_PAGE_ANIMATION_DURATION_MILLIS = 90
