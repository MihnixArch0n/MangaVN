package com.example.mybookslibrary.ui.screens.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmedEdgeTapTrackerTest {

    @Test
    fun `same position double tap is suppressed so zoom can handle it`() {
        val confirmedTaps = mutableListOf<Offset>()
        val tracker = ConfirmedEdgeTapTracker(
            touchSlop = 12f,
            onConfirmedEdgeTap = confirmedTaps::add
        )
        val tap = Offset(900f, 1200f)

        assertEquals(EdgeTapConfirmation.Pending, tracker.onTap(tap))
        assertEquals(EdgeTapConfirmation.SuppressedDoubleTap, tracker.onTap(tap.copy(x = 906f)))

        assertTrue(confirmedTaps.isEmpty())
        assertFalse(tracker.confirmPendingTap(tap))
    }

    @Test
    fun `distinct quick tap confirms previous tap and keeps new tap pending`() {
        val confirmedTaps = mutableListOf<Offset>()
        val tracker = ConfirmedEdgeTapTracker(
            touchSlop = 12f,
            onConfirmedEdgeTap = confirmedTaps::add
        )
        val firstTap = Offset(900f, 1200f)
        val secondTap = Offset(900f, 1240f)

        assertEquals(EdgeTapConfirmation.Pending, tracker.onTap(firstTap))
        assertEquals(EdgeTapConfirmation.ConfirmedPrevious, tracker.onTap(secondTap))
        assertEquals(listOf(firstTap), confirmedTaps)

        assertTrue(tracker.confirmPendingTap(secondTap))
        assertEquals(listOf(firstTap, secondTap), confirmedTaps)
    }

    @Test
    fun `cancel pending tap prevents delayed confirmation`() {
        val confirmedTaps = mutableListOf<Offset>()
        val tracker = ConfirmedEdgeTapTracker(
            touchSlop = 12f,
            onConfirmedEdgeTap = confirmedTaps::add
        )
        val tap = Offset(900f, 1200f)

        assertEquals(EdgeTapConfirmation.Pending, tracker.onTap(tap))
        tracker.cancelPendingTap()

        assertFalse(tracker.confirmPendingTap(tap))
        assertTrue(confirmedTaps.isEmpty())
    }
}
