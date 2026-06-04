package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * PBT bổ sung cho [TapZoneEvaluator]: invariants chưa có trong TapZoneEvaluatorPropertyTest.
 * Guard non-finite/zero, LTR zone boundaries, RTL vs LTR đối xứng, coerce out-of-bound.
 *
 * Dùng block body (KHÔNG expression body `= runBlocking{}`) vì checkAll trả PropertyContext
 * → method không phải void → JUnit4 ném InvalidTestClassError với expression body.
 */
class TapZoneEvaluatorPropertyTestExtended {
    private val evaluator = TapZoneEvaluator()

    @Test
    fun nonFiniteInput_luonTraNone() {
        runBlocking {
            checkAll(Arb.enum<ReadingMode>()) { mode ->
                assertEquals(ReaderTapAction.NONE, evaluator(Float.NaN, 0f, 100f, 100f, mode))
                assertEquals(ReaderTapAction.NONE, evaluator(0f, Float.POSITIVE_INFINITY, 100f, 100f, mode))
                assertEquals(ReaderTapAction.NONE, evaluator(0f, 0f, Float.NaN, 100f, mode))
                assertEquals(ReaderTapAction.NONE, evaluator(0f, 0f, 100f, Float.NEGATIVE_INFINITY, mode))
            }
        }
    }

    @Test
    fun zeroScreenSize_luonTraNone() {
        runBlocking {
            checkAll(
                Arb.numericFloat(min = 0f, max = 100f),
                Arb.numericFloat(min = 0f, max = 100f)
            ) { x, y ->
                assertEquals(ReaderTapAction.NONE, evaluator(x, y, 0f, 100f, ReadingMode.LTR))
                assertEquals(ReaderTapAction.NONE, evaluator(x, y, 100f, 0f, ReadingMode.LTR))
            }
        }
    }

    @Test
    fun ltr_tapTrai10Pct_luonPreviousPage() {
        runBlocking {
            checkAll(
                Arb.numericFloat(min = 1f, max = 1000f).filterNot { it <= 0f }
            ) { width ->
                val xLeft = width * 0.10f
                assertEquals(ReaderTapAction.PREVIOUS_PAGE, evaluator(xLeft, 50f, width, 100f, ReadingMode.LTR))
            }
        }
    }

    @Test
    fun ltr_tapPhai90Pct_luonNextPage() {
        runBlocking {
            checkAll(Arb.numericFloat(min = 1f, max = 1000f).filterNot { it <= 0f }) { width ->
                val xRight = width * 0.90f
                assertEquals(ReaderTapAction.NEXT_PAGE, evaluator(xRight, 50f, width, 100f, ReadingMode.LTR))
            }
        }
    }

    @Test
    fun ltrVaRtl_nguocChieuOVungBien() {
        runBlocking {
            checkAll(Arb.numericFloat(min = 1f, max = 1000f).filterNot { it <= 0f }) { width ->
                val xLeft = width * 0.10f
                val xRight = width * 0.90f
                assertNotEquals(
                    evaluator(xLeft, 50f, width, 100f, ReadingMode.LTR),
                    evaluator(xLeft, 50f, width, 100f, ReadingMode.RTL)
                )
                assertNotEquals(
                    evaluator(xRight, 50f, width, 100f, ReadingMode.LTR),
                    evaluator(xRight, 50f, width, 100f, ReadingMode.RTL)
                )
            }
        }
    }

    @Test
    fun xNgoaiBien_gioiHanChoKetQuaGiongClamp() {
        runBlocking {
            checkAll(Arb.numericFloat(min = 1f, max = 1000f).filterNot { it <= 0f }) { width ->
                assertEquals(
                    evaluator(0f, 50f, width, 100f, ReadingMode.LTR),
                    evaluator(-100f, 50f, width, 100f, ReadingMode.LTR)
                )
            }
        }
    }

    @Test
    fun middleZone_luonToggleOverlay() {
        runBlocking {
            checkAll(Arb.numericFloat(min = 1f, max = 1000f).filterNot { it <= 0f }) { width ->
                val xMid = width * 0.50f
                assertEquals(ReaderTapAction.TOGGLE_OVERLAY, evaluator(xMid, 50f, width, 100f, ReadingMode.LTR))
                assertEquals(ReaderTapAction.TOGGLE_OVERLAY, evaluator(xMid, 50f, width, 100f, ReadingMode.RTL))
            }
        }
    }
}
