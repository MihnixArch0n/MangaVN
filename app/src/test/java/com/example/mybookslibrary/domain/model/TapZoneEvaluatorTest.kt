package com.example.mybookslibrary.domain.model

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

    private val totalWidth = 1000f

    // ──────────────────────────────────────────────
    // LTR Mode
    // ──────────────────────────────────────────────

    @Test
    fun `LTR - tap at 0 percent returns PREVIOUS_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 0f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `LTR - tap at 24_9 percent returns PREVIOUS_PAGE`() {
        // Just before the 25% boundary
        val result = TapZoneEvaluator.evaluateTap(x = 249f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `LTR - tap at exactly 25 percent returns TOGGLE_OVERLAY`() {
        // At exactly 25% boundary → center zone
        val result = TapZoneEvaluator.evaluateTap(x = 250f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at 50 percent returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 500f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at 74_9 percent returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 749f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `LTR - tap at exactly 75 percent returns NEXT_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 750f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `LTR - tap at 100 percent returns NEXT_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 1000f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    // ──────────────────────────────────────────────
    // RTL Mode (directions reversed)
    // ──────────────────────────────────────────────

    @Test
    fun `RTL - tap at 0 percent returns NEXT_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 0f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `RTL - tap at 24_9 percent returns NEXT_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 249f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `RTL - tap at exactly 25 percent returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 250f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `RTL - tap at 50 percent returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 500f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `RTL - tap at exactly 75 percent returns PREVIOUS_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 750f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `RTL - tap at 100 percent returns PREVIOUS_PAGE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 1000f, totalWidth = totalWidth, mode = ReadingMode.RTL)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    // ──────────────────────────────────────────────
    // VERTICAL Mode — all taps toggle overlay
    // ──────────────────────────────────────────────

    @Test
    fun `VERTICAL - tap at left zone returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 100f, totalWidth = totalWidth, mode = ReadingMode.VERTICAL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `VERTICAL - tap at center returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 500f, totalWidth = totalWidth, mode = ReadingMode.VERTICAL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    @Test
    fun `VERTICAL - tap at right zone returns TOGGLE_OVERLAY`() {
        val result = TapZoneEvaluator.evaluateTap(x = 900f, totalWidth = totalWidth, mode = ReadingMode.VERTICAL)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, result)
    }

    // ──────────────────────────────────────────────
    // Edge Cases
    // ──────────────────────────────────────────────

    @Test
    fun `zero totalWidth returns NONE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 50f, totalWidth = 0f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun `negative totalWidth returns NONE`() {
        val result = TapZoneEvaluator.evaluateTap(x = 50f, totalWidth = -100f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun `negative x is clamped to 0 - left zone`() {
        // Negative x should be clamped to 0, which is in left zone
        val result = TapZoneEvaluator.evaluateTap(x = -50f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, result)
    }

    @Test
    fun `x exceeding totalWidth is clamped to totalWidth - right zone`() {
        val result = TapZoneEvaluator.evaluateTap(x = 1500f, totalWidth = totalWidth, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, result)
    }

    @Test
    fun `small totalWidth - zones still calculated correctly`() {
        // totalWidth = 4px → left zone is [0, 1), center [1, 3), right [3, 4]
        val leftResult = TapZoneEvaluator.evaluateTap(x = 0f, totalWidth = 4f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, leftResult)

        val centerResult = TapZoneEvaluator.evaluateTap(x = 2f, totalWidth = 4f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, centerResult)

        val rightResult = TapZoneEvaluator.evaluateTap(x = 3f, totalWidth = 4f, mode = ReadingMode.LTR)
        assertEquals(ReaderTapAction.NEXT_PAGE, rightResult)
    }
}
