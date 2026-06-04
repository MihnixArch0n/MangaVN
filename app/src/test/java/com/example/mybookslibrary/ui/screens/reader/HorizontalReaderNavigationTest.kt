package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HorizontalReaderNavigationTest {

    @Test
    fun `LTR edge taps map to previous and next pages`() {
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, evaluateHorizontalTap(100f, 1000f, ReadingMode.LTR))
        assertEquals(ReaderTapAction.NEXT_PAGE, evaluateHorizontalTap(900f, 1000f, ReadingMode.LTR))
    }

    @Test
    fun `RTL edge taps reverse previous and next pages`() {
        assertEquals(ReaderTapAction.NEXT_PAGE, evaluateHorizontalTap(100f, 1000f, ReadingMode.RTL))
        assertEquals(ReaderTapAction.PREVIOUS_PAGE, evaluateHorizontalTap(900f, 1000f, ReadingMode.RTL))
    }

    @Test
    fun `center tap toggles overlay`() {
        assertEquals(ReaderTapAction.TOGGLE_OVERLAY, evaluateHorizontalTap(500f, 1000f, ReadingMode.LTR))
    }

    @Test
    fun `invalid tap dimensions return none`() {
        assertEquals(ReaderTapAction.NONE, evaluateHorizontalTap(100f, 0f, ReadingMode.LTR))
        assertEquals(ReaderTapAction.NONE, evaluateHorizontalTap(Float.NaN, 1000f, ReadingMode.LTR))
    }

    @Test
    fun `target calculation clamps at chapter boundaries`() {
        assertEquals(0, calculateHorizontalTargetPage(0, ReaderTapAction.PREVIOUS_PAGE, 7))
        assertEquals(7, calculateHorizontalTargetPage(7, ReaderTapAction.NEXT_PAGE, 7))
    }

    @Test
    fun `target calculation accumulates from active pager target`() {
        assertEquals(4, calculateHorizontalTargetPage(3, ReaderTapAction.NEXT_PAGE, 7))
        assertEquals(2, calculateHorizontalTargetPage(3, ReaderTapAction.PREVIOUS_PAGE, 7))
    }

    @Test
    fun `target calculation ignores non-navigation actions and empty chapters`() {
        assertNull(calculateHorizontalTargetPage(3, ReaderTapAction.TOGGLE_OVERLAY, 7))
        assertNull(calculateHorizontalTargetPage(3, ReaderTapAction.NONE, 7))
        assertNull(calculateHorizontalTargetPage(0, ReaderTapAction.NEXT_PAGE, -1))
    }

    @Test
    fun `animation uses default duration for adjacent page`() {
        assertEquals(DEFAULT_PAGE_ANIMATION_DURATION_MILLIS, horizontalPageAnimationDurationMillis(2, 3))
    }

    @Test
    fun `animation uses fast duration for accumulated target`() {
        assertEquals(FAST_PAGE_ANIMATION_DURATION_MILLIS, horizontalPageAnimationDurationMillis(2, 4))
    }

    @Test
    fun `animation uses fast duration for retargeted adjacent page`() {
        assertEquals(
            FAST_PAGE_ANIMATION_DURATION_MILLIS,
            horizontalPageAnimationDurationMillis(currentPage = 2, nextPage = 3, isRetargeted = true)
        )
    }
}
