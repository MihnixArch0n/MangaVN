package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import javax.inject.Inject

/**
 * Maps raw reader tap coordinates to a logical reader action.
 *
 * This use case is pure Kotlin and intentionally has no Android framework
 * dependencies so it can be injected and tested independently.
 */
class TapZoneEvaluator
    @Inject
    constructor() {
        /**
         * Chuyển tọa độ tap thành [ReaderTapAction].
         *
         * Vùng trái (<25% width) và phải (>75% width) là PREVIOUS/NEXT_PAGE (đảo chiều cho RTL).
         * Vùng giữa và toàn bộ VERTICAL mode → TOGGLE_OVERLAY.
         * Input không hợp lệ (NaN, kích thước ≤ 0) → [ReaderTapAction.NONE].
         */
        operator fun invoke(
            x: Float,
            y: Float,
            screenWidth: Float,
            screenHeight: Float,
            mode: ReadingMode,
        ): ReaderTapAction {
            if (!x.isFinite() || !y.isFinite() || !screenWidth.isFinite() || !screenHeight.isFinite()) {
                return ReaderTapAction.NONE
            }
            if (screenWidth <= 0f || screenHeight <= 0f) return ReaderTapAction.NONE

            val clampedX = x.coerceIn(0f, screenWidth)
            val ratio = clampedX / screenWidth

            if (mode == ReadingMode.VERTICAL) return ReaderTapAction.TOGGLE_OVERLAY

            // VERTICAL đã được xử lý ở trên — chỉ còn LTR/RTL
            return when {
                ratio < LEFT_ZONE_END_RATIO ->
                    if (mode == ReadingMode.LTR) {
                        ReaderTapAction.PREVIOUS_PAGE
                    } else {
                        ReaderTapAction.NEXT_PAGE
                    }
                ratio >= RIGHT_ZONE_START_RATIO ->
                    if (mode == ReadingMode.LTR) {
                        ReaderTapAction.NEXT_PAGE
                    } else {
                        ReaderTapAction.PREVIOUS_PAGE
                    }
                else -> ReaderTapAction.TOGGLE_OVERLAY
            }
        }

        private companion object {
            private const val LEFT_ZONE_END_RATIO = 0.25f
            private const val RIGHT_ZONE_START_RATIO = 0.75f
        }
    }
