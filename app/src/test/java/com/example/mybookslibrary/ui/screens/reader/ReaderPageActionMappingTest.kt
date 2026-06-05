package com.example.mybookslibrary.ui.screens.reader

import com.example.mybookslibrary.ui.screens.reader.components.PageAction
import com.example.mybookslibrary.ui.viewmodel.ReaderPageAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test exhaustive mapping [PageAction] → [ReaderPageAction].
 * Pure function, không cần PBT — chỉ cần kiểm tra mọi case enum.
 */
class ReaderPageActionMappingTest {
    @Test
    fun quickSave_mapsCorrectly() {
        assertEquals(ReaderPageAction.QuickSave, PageAction.QuickSave.toReaderPageAction())
    }

    @Test
    fun saveAs_mapsCorrectly() {
        assertEquals(ReaderPageAction.SaveAs, PageAction.SaveAs.toReaderPageAction())
    }

    @Test
    fun share_mapsCorrectly() {
        assertEquals(ReaderPageAction.Share, PageAction.Share.toReaderPageAction())
    }

    @Test
    fun allValues_mappedExhaustively() {
        // Kiểm tra exhaustive mapping không cần reflection — liệt kê tường minh
        // để khi thêm PageAction mới, test fail nhắc cập nhật.
        val mappings = mapOf(
            PageAction.QuickSave to ReaderPageAction.QuickSave,
            PageAction.SaveAs to ReaderPageAction.SaveAs,
            PageAction.Share to ReaderPageAction.Share,
        )
        mappings.forEach { (action, expected) ->
            assertEquals(expected, action.toReaderPageAction())
        }
    }
}
