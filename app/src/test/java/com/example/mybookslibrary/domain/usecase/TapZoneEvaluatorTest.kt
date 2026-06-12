@file:Suppress("ktlint")

package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [TapZoneEvaluator].
 *
 * Zone layout (percentage of totalWidth):
 * - Left:   [0%, 25%)
 * - Center: [25%, 75%)
 * - Right:  [75%, 100%]
 *
 * Tests cover boundary conditions, all three [ReadingMode] values,
 * and edge cases like zero/negative width and out-of-bounds coordinates.
 */
class TapZoneEvaluatorTest {
    private val evaluator = TapZoneEvaluator()
    private val totalWidth = 1000f
    private val totalHeight = 1000f

    // ──────────────────────────────────────────────
    // LTR Mode
    // ──────────────────────────────────────────────

    @Test
    fun `LTR - tap at 0 percent returns PREVIOUS_PAGE`() {
        val result =
            evaluator(x = 0f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `LTR - tap at 24_9 percent returns PREVIOUS_PAGE`() {
        // Just before the 25% boundary
        val result =
            evaluator(x = 249f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `LTR - tap at exactly 25 percent returns TOGGLE_OVERLAY`() {
        // At exactly 25% boundary → center zone
        val result =
            evaluator(x = 250f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at 50 percent returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(x = 500f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at 74_9 percent returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(x = 749f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at exactly 75 percent returns NEXT_PAGE`() {
        val result =
            evaluator(x = 750f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `LTR - tap at 100 percent returns NEXT_PAGE`() {
        val result =
            evaluator(x = 1000f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    // ──────────────────────────────────────────────
    // RTL Mode (directions reversed)
    // ──────────────────────────────────────────────

    @Test
    fun `RTL - tap at 0 percent returns NEXT_PAGE`() {
        val result =
            evaluator(x = 0f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `RTL - tap at 24_9 percent returns NEXT_PAGE`() {
        val result =
            evaluator(x = 249f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `RTL - tap at exactly 25 percent returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(x = 250f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `RTL - tap at 50 percent returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(x = 500f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `RTL - tap at exactly 75 percent returns PREVIOUS_PAGE`() {
        val result =
            evaluator(x = 750f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `RTL - tap at 100 percent returns PREVIOUS_PAGE`() {
        val result =
            evaluator(x = 1000f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    // ──────────────────────────────────────────────
    // VERTICAL Mode — all taps toggle overlay
    // ──────────────────────────────────────────────

    @Test
    fun `VERTICAL - tap at left zone returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(
                x = 100f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.VERTICAL,
            )
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `VERTICAL - tap at center returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(
                x = 500f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.VERTICAL,
            )
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `VERTICAL - tap at right zone returns TOGGLE_OVERLAY`() {
        val result =
            evaluator(
                x = 900f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.VERTICAL,
            )
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    // ──────────────────────────────────────────────
    // Edge Cases
    // ──────────────────────────────────────────────

    @Test
    fun `zero totalWidth returns NONE`() {
        val result = evaluator(x = 50f, y = 500f, screenWidth = 0f, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun `negative totalWidth returns NONE`() {
        val result =
            evaluator(x = 50f, y = 500f, screenWidth = -100f, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun `zero screenHeight returns NONE`() {
        val result = evaluator(x = 50f, y = 500f, screenWidth = totalWidth, screenHeight = 0f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun `negative x is clamped to 0 - left zone`() {
        // Negative x should be clamped to 0, which is in left zone
        val result =
            evaluator(x = -50f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `x exceeding totalWidth is clamped to totalWidth - right zone`() {
        val result =
            evaluator(x = 1500f, y = 500f, screenWidth = totalWidth, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `small totalWidth - zones still calculated correctly`() {
        // totalWidth = 4px → left zone is [0, 1), center [1, 3), right [3, 4]
        val leftResult =
            evaluator(x = 0f, y = 500f, screenWidth = 4f, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, leftResult)

        val centerResult =
            evaluator(x = 2f, y = 500f, screenWidth = 4f, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, centerResult)

        val rightResult =
            evaluator(x = 3f, y = 500f, screenWidth = 4f, screenHeight = totalHeight, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, rightResult)
    }
}
