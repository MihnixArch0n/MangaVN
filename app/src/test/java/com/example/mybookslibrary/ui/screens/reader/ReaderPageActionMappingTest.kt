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
        // Đảm bảo mọi PageAction đều có mapping (không bỏ sót khi thêm case mới)
        PageAction::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance
                ?: error("PageAction có data class? Cần cập nhật test.")
            val result = instance.toReaderPageAction()
            assert(result is ReaderPageAction) {
                "${subclass.simpleName} không map được sang ReaderPageAction"
            }
        }
    }
}
