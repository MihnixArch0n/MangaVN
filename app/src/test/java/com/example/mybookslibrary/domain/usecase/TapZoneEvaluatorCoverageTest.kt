package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phủ nhánh tọa độ/kích thước không hợp lệ (NaN/Infinity) của [TapZoneEvaluator].
 */
class TapZoneEvaluatorCoverageTest {
    private val evaluator = TapZoneEvaluator()

    @Test
    fun nonFiniteX_returnsNone() {
        val result =
            evaluator(
                x = Float.NaN,
                y = 500f,
                screenWidth = 1000f,
                screenHeight = 1000f,
                mode = ReadingMode.LTR,
            )
        assertEquals(ReaderTapAction.NONE, result)
    }

    @Test
    fun infiniteScreenWidth_returnsNone() {
        val result =
            evaluator(
                x = 100f,
                y = 500f,
                screenWidth = Float.POSITIVE_INFINITY,
                screenHeight = 1000f,
                mode = ReadingMode.LTR,
            )
        assertEquals(ReaderTapAction.NONE, result)
    }
}
