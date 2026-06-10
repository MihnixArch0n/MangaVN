package com.example.mybookslibrary.data.download

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/**
 * Phủ các NHÁNH lỗi/biên của [OfflineDownloadStorage] mà happy-path test bỏ qua:
 * lỗi tạo thư mục, complete khi rỗng, backfill set rỗng, lọc file rác khi scan,
 * sanitize extension/segment, ghi lại trang đã có, và marker đã tồn tại.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadStorageBranchTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun storage() = OfflineDownloadStorage(context, UnconfinedTestDispatcher())

    private fun bytes() = ByteArrayInputStream(byteArrayOf(1, 2, 3))

    private fun rootDir() = File(context.filesDir, "offline_manga")

    @Test
    fun savePage_khongTaoDuocThuMuc_nemIOException() =
        runTest {
            // Đặt 1 FILE ở vị trí thư mục manga -> mkdirs() cho chapterDir thất bại.
            rootDir().mkdirs()
            val blocker = File(rootDir(), "blocker-manga")
            blocker.parentFile?.mkdirs()
            blocker.writeText("not a dir")

            assertThrows(IOException::class.java) {
                runBlockingIO { storage().savePage("blocker-manga", "ch", 0, bytes()) }
            }
            blocker.delete()
        }

    @Test
    fun markChapterComplete_khongCoTrang_nemIOException() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA, "no-pages")

            assertThrows(IOException::class.java) {
                runBlockingIO { storage.markChapterComplete(MANGA, "no-pages", totalPages = 1) }
            }
        }

    @Test
    fun backfill_setRong_traVe0() =
        runTest {
            assertEquals(0, storage().backfillCompletionMarkers(emptySet()))
        }

    @Test
    fun scan_loaiThuMucCoMarkerNhungKhongCoTrang() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA, "marker-only")
            // Tạo thư mục chỉ có marker, không có trang hợp lệ.
            val dir = File(File(rootDir(), MANGA), "marker-only").apply { mkdirs() }
            File(dir, ".complete").createNewFile()

            assertFalse("marker-only" in storage.scanDownloadedChapters())
            assertFalse(storage.verifyDownloadedChapter(MANGA, "marker-only"))
            storage.deleteChapter(MANGA, "marker-only")
        }

    @Test
    fun getChapterPages_boQuaTempVaFileLa_thuMucKhongTonTaiTraRong() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA, CHAPTER)
            // Thư mục không tồn tại -> listFiles null -> orEmpty
            assertTrue(storage.getChapterPages(MANGA, "khong-ton-tai").isEmpty())

            storage.savePage(MANGA, CHAPTER, 0, bytes())
            val dir = File(File(rootDir(), MANGA), CHAPTER)
            File(dir, "page_00001.img.tmp").createNewFile() // bị loại vì .tmp
            File(dir, "garbage.txt").createNewFile() // bị loại vì không prefix page_
            File(dir, "page_xx.img").createNewFile() // index không số -> sort MAX

            val pages = storage.getChapterPages(MANGA, CHAPTER)
            assertTrue(pages.none { it.name.endsWith(".tmp") })
            assertTrue(pages.none { it.name == "garbage.txt" })
            storage.deleteChapter(MANGA, CHAPTER)
        }

    @Test
    fun savePage_extensionRong_dungImg_vaSegmentRongDungUnknown() =
        runTest {
            val storage = storage()
            // extension toàn ký tự lạ -> sanitize ra rỗng -> "img"
            val file = storage.savePage(MANGA, "ext-test", 0, bytes(), extension = "###")
            assertTrue(file.name.endsWith(".img"))
            storage.deleteChapter(MANGA, "ext-test")

            // mangaId rỗng -> safeSegment ifBlank -> "unknown"
            val f2 = storage.savePage("", "blank-manga", 0, bytes())
            assertTrue(f2.absolutePath.replace('\\', '/').contains("/unknown/"))
            storage.deleteChapter("", "blank-manga")
        }

    @Test
    fun savePage_ghiLaiTrangDaCo_thayThe() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA, "resave")
            storage.savePage(MANGA, "resave", 0, bytes())
            // Lần 2 cùng index -> pageFile.exists() true -> delete + rename
            val file = storage.savePage(MANGA, "resave", 0, ByteArrayInputStream(byteArrayOf(9, 9)))
            assertTrue(file.exists())
            storage.deleteChapter(MANGA, "resave")
        }

    @Test
    fun markChapterComplete_goiHaiLan_markerDaTonTai() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA, "twice")
            storage.savePage(MANGA, "twice", 0, bytes())

            storage.markChapterComplete(MANGA, "twice", totalPages = 1)
            // Lần 2: marker đã tồn tại -> nhánh !marker.exists() false
            storage.markChapterComplete(MANGA, "twice", totalPages = 1)
            assertTrue("twice" in storage.scanDownloadedChapters())
            storage.deleteChapter(MANGA, "twice")
        }

    @Test
    fun backfill_chiMarkDungThuMucHopLe() =
        runTest {
            val storage = storage()
            listOf("bf-in", "bf-out", "bf-marked", "bf-empty").forEach { storage.deleteChapter(MANGA, it) }

            // bf-in: trong set, có trang, chưa marker -> được mark
            storage.savePage(MANGA, "bf-in", 0, bytes())
            // bf-out: KHÔNG trong set, có trang -> bỏ qua (name in set = false)
            storage.savePage(MANGA, "bf-out", 0, bytes())
            // bf-marked: trong set, có trang, ĐÃ marker -> bỏ qua (!marker.exists() = false)
            storage.savePage(MANGA, "bf-marked", 0, bytes())
            storage.markChapterComplete(MANGA, "bf-marked", totalPages = 1)
            // bf-empty: trong set, KHÔNG trang -> bỏ qua (pages.isNotEmpty() = false)
            File(File(rootDir(), MANGA), "bf-empty").mkdirs()

            val created = storage.backfillCompletionMarkers(setOf("bf-in", "bf-marked", "bf-empty"))

            assertEquals(1, created)
            assertTrue("bf-in" in storage.scanDownloadedChapters())
            listOf("bf-in", "bf-out", "bf-marked", "bf-empty").forEach { storage.deleteChapter(MANGA, it) }
        }

    // savePage/markChapterComplete là suspend chạy trên ioDispatcher; bọc runBlocking để
    // assertThrows bắt được exception đồng bộ.
    private fun <T> runBlockingIO(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }

    private companion object {
        const val MANGA = "storage-branch-manga"
        const val CHAPTER = "storage-branch-chapter"
    }
}
