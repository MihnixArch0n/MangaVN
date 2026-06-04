package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.domain.model.ReadingMode
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Property-based test cho [TapZoneEvaluator].
 *
 * Khác example-based test (kiểm vài điểm cố định): ở đây ta khẳng định các INVARIANT
 * rồi để kotest sinh hàng nghìn input ngẫu nhiên dò phản ví dụ — bắt được edge case mà
 * test thủ công bỏ sót (đặc biệt: test cũ hardcode y=500f nên bug phụ thuộc y sẽ lọt lưới).
 *
 * Dùng block body + runBlocking để method test trả về Unit (JUnit4 yêu cầu test method void;
 * checkAll trả về PropertyContext nên KHÔNG được dùng expression body `= runBlocking {...}`).
 */
class TapZoneEvaluatorPropertyTest {
    private val evaluator = TapZoneEvaluator()

    // Toạ độ/kích thước hợp lệ: width/height > 0; x gồm cả ngoài biên để ép nhánh clamp.
    private val width = Arb.numericFloat(1f, 5000f)
    private val height = Arb.numericFloat(1f, 5000f)
    private val coordX = Arb.numericFloat(-2000f, 7000f)
    private val coordY = Arb.numericFloat(0f, 5000f)
    private val modes = Arb.enum<ReadingMode>()

    /** Invariant 1: LTR và RTL đối xứng — đổi PREVIOUS↔NEXT, TOGGLE/NONE giữ nguyên. */
    @Test
    fun `LTR và RTL luôn đối xứng với mọi toạ độ`() {
        runBlocking {
            checkAll(2000, coordX, coordY, width, height) { x, y, w, h ->
                val ltr = evaluator(x, y, w, h, ReadingMode.LTR)
                val rtl = evaluator(x, y, w, h, ReadingMode.RTL)
                assertEquals(mirror(ltr), rtl)
            }
        }
    }

    /** Invariant 2: chỉ x quyết định zone — đổi y và screenHeight KHÔNG được đổi action. */
    @Test
    fun `đổi y và screenHeight không làm đổi action`() {
        runBlocking {
            checkAll(2000, coordX, width, coordY, coordY, height, height, modes) { x, w, y1, y2, h1, h2, mode ->
                assertEquals(
                    evaluator(x, y1, w, h1, mode),
                    evaluator(x, y2, w, h2, mode),
                )
            }
        }
    }

    /** Invariant 3: VERTICAL luôn trả về TOGGLE_OVERLAY khi input hợp lệ. */
    @Test
    fun `VERTICAL luôn trả về TOGGLE_OVERLAY`() {
        runBlocking {
            checkAll(1000, coordX, coordY, width, height) { x, y, w, h ->
                assertEquals(
                    ReaderTapAction.TOGGLE_OVERLAY,
                    evaluator(x, y, w, h, ReadingMode.VERTICAL),
                )
            }
        }
    }

    // Ánh xạ kết quả LTR sang kết quả RTL kỳ vọng (chỉ đảo hướng trang).
    private fun mirror(action: ReaderTapAction): ReaderTapAction =
        when (action) {
            ReaderTapAction.PREVIOUS_PAGE -> ReaderTapAction.NEXT_PAGE
            ReaderTapAction.NEXT_PAGE -> ReaderTapAction.PREVIOUS_PAGE
            ReaderTapAction.TOGGLE_OVERLAY -> ReaderTapAction.TOGGLE_OVERLAY
            ReaderTapAction.NONE -> ReaderTapAction.NONE
        }
}
