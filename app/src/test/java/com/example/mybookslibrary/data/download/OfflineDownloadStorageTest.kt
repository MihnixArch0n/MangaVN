package com.example.mybookslibrary.data.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadStorageTest {
    @Test
    fun scanDownloadedChapters_requiresCompletionMarkerAndPage() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                assertFalse(CHAPTER_ID in storage.scanDownloadedChapters())

                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                assertTrue(CHAPTER_ID in storage.scanDownloadedChapters())
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun verifyDownloadedChapter_requiresCompletionMarkerAndPage() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                assertTrue(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun backfillCompletionMarkers_marksLegacyDirectoryOnce() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, LEGACY_CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())

                assertEquals(1, storage.backfillCompletionMarkers(setOf(LEGACY_CHAPTER_ID)))
                assertEquals(0, storage.backfillCompletionMarkers(setOf(LEGACY_CHAPTER_ID)))
                assertTrue(LEGACY_CHAPTER_ID in storage.scanDownloadedChapters())
            } finally {
                storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)
            }
        }

    @Test
    fun scanCorruptedChapters_findsChaptersWithMissingPages() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                // Not corrupted if nothing is downloaded
                assertTrue(storage.scanCorruptedChapters().isEmpty())

                // Download 2 pages, mark as complete (total = 2)
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val page1 = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 1, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 2)

                // Not corrupted, perfectly valid
                assertTrue(storage.scanCorruptedChapters().isEmpty())

                // Simulate external deletion of page 1
                page1.delete()

                // Now it should be considered corrupted
                val corrupted = storage.scanCorruptedChapters()
                assertEquals(1, corrupted.size)
                assertEquals(Pair(MANGA_ID, CHAPTER_ID), corrupted[0])
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun getPageFileIfExists_returnsFileIfValid() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                val file = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())

                assertEquals(file.absolutePath, storage.getPageFileIfExists(MANGA_ID, CHAPTER_ID, 0)?.absolutePath)
                assertEquals(null, storage.getPageFileIfExists(MANGA_ID, CHAPTER_ID, 1))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    // ---- Branch coverage cho các nhánh edge-case của verify/scan (marker hợp lệ
    // nhưng nội dung không phải số, page count lệch marker, page index có lỗ hổng) ----

    @Test
    fun verifyDownloadedChapter_acceptsNonNumericMarkerWhenPagesContiguous() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                val page = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                markerFile(page).writeText("legacy-marker-khong-phai-so")

                assertTrue(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun verifyDownloadedChapter_rejectsWhenPageCountMismatchesMarker() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 2)

                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun verifyDownloadedChapter_rejectsWhenPageIndexesHaveGap() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val lastPage = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 2, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 2)

                // size=2 khớp marker nhưng index cuối là 2 (lỗ hổng tại index 1) → invalid
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))

                // Nhánh fallback (marker không phải số) cũng phải reject lỗ hổng index
                markerFile(lastPage).writeText("khong-phai-so")
                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun scanDownloadedChapters_acceptsNonNumericMarkerAndRejectsMismatch() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)

            try {
                // Chapter hợp lệ với marker non-numeric (legacy)
                val validPage = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                markerFile(validPage).writeText("legacy")

                // Chapter lệch count: 1 page nhưng marker ghi 3
                storage.savePage(MANGA_ID, LEGACY_CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, LEGACY_CHAPTER_ID, totalPages = 3)

                val downloaded = storage.scanDownloadedChapters()
                assertTrue(CHAPTER_ID in downloaded)
                assertFalse(LEGACY_CHAPTER_ID in downloaded)
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
                storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)
            }
        }

    @Test
    fun scanCorruptedChapters_flagsMarkerWithoutPagesAndNonNumericGap() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)

            try {
                // Chapter có marker nhưng toàn bộ page bị xóa ngoài luồng
                val orphanPage = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                orphanPage.delete()

                // Chapter marker non-numeric + page index có lỗ hổng
                storage.savePage(MANGA_ID, LEGACY_CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val gapPage = storage.savePage(MANGA_ID, LEGACY_CHAPTER_ID, pageIndex = 2, byteStream = pageBytes())
                markerFile(gapPage).writeText("legacy")

                val corrupted = storage.scanCorruptedChapters()
                assertTrue(Pair(MANGA_ID, CHAPTER_ID) in corrupted)
                assertTrue(Pair(MANGA_ID, LEGACY_CHAPTER_ID) in corrupted)
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
                storage.deleteChapter(MANGA_ID, LEGACY_CHAPTER_ID)
            }
        }

    @Test
    fun scanCorruptedChapters_acceptsNonNumericMarkerWhenContiguous() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val page1 = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 1, byteStream = pageBytes())
                markerFile(page1).writeText("legacy")

                assertTrue(storage.scanCorruptedChapters().isEmpty())
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun savePage_overwritesExistingPageFile() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                val overwritten =
                    storage.savePage(
                        MANGA_ID,
                        CHAPTER_ID,
                        pageIndex = 0,
                        byteStream = ByteArrayInputStream(byteArrayOf(9, 9)),
                    )

                assertEquals(1, storage.getChapterPages(MANGA_ID, CHAPTER_ID).size)
                assertEquals(2, overwritten.length().toInt())
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun getPageFileIfExists_ignoresZeroBytePage() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = ByteArrayInputStream(ByteArray(0)))

                assertEquals(null, storage.getPageFileIfExists(MANGA_ID, CHAPTER_ID, 0))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun scanChapterStates_marksCompletedChapterWithZeroBytePageAsCorrupted() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                val page = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                page.writeBytes(ByteArray(0))

                val result = storage.scanChapterStates()

                assertFalse(CHAPTER_ID in result.downloadedChapterIds)
                assertTrue((MANGA_ID to CHAPTER_ID) in result.corruptedChapters)
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun verifyDownloadedChapter_rejectsMarkerWithoutPages() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                val page = storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.markChapterComplete(MANGA_ID, CHAPTER_ID, totalPages = 1)
                page.delete()

                assertFalse(storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun backfillCompletionMarkers_skipsChapterWithPageGap() =
        runTest {
            val storage = storage()
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)

            try {
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 0, byteStream = pageBytes())
                storage.savePage(MANGA_ID, CHAPTER_ID, pageIndex = 2, byteStream = pageBytes())

                // Lỗ hổng index (0, 2) → không được coi là download hoàn chỉnh legacy
                assertEquals(0, storage.backfillCompletionMarkers(setOf(CHAPTER_ID)))
            } finally {
                storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            }
        }

    @Test
    fun savePage_sanitizesBlankSegmentsAndExtension() =
        runTest {
            val storage = storage()
            storage.deleteChapter("", CHAPTER_ID)

            try {
                // mangaId rỗng → segment "unknown"; extension toàn ký tự đặc biệt → "img"
                val page = storage.savePage("", CHAPTER_ID, pageIndex = 0, byteStream = pageBytes(), extension = "??")

                assertTrue(page.name.endsWith(".img"))
                assertEquals("unknown", page.parentFile?.parentFile?.name)
            } finally {
                storage.deleteChapter("", CHAPTER_ID)
            }
        }

    @Test
    fun savePage_concurrentFirstWritesIntoNewChapterDir_neverThrows() =
        runTest {
            // Regression cho issue #92: race TOCTOU giữa exists() và mkdirs() — nhiều thread
            // cùng ghi trang đầu tiên vào chapter dir chưa tồn tại thì mkdirs() của thread
            // thua cuộc trả false (dir vừa bị thread khác tạo) và bị throw IOException oan,
            // kéo theo AtHome failover refresh thừa. Lặp nhiều vòng để nới cửa sổ race.
            val storage =
                OfflineDownloadStorage(
                    context = RuntimeEnvironment.getApplication(),
                    ioDispatcher = Dispatchers.IO,
                )
            repeat(RACE_ITERATIONS) { iteration ->
                val chapterId = "race-chapter-$iteration"
                try {
                    val startBarrier = CyclicBarrier(CONCURRENT_WRITERS)
                    (0 until CONCURRENT_WRITERS)
                        .map { pageIndex ->
                            async(Dispatchers.IO) {
                                startBarrier.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                storage.savePage(MANGA_ID, chapterId, pageIndex, pageBytes())
                            }
                        }.awaitAll()
                    assertEquals(CONCURRENT_WRITERS, storage.getChapterPages(MANGA_ID, chapterId).size)
                } finally {
                    storage.deleteChapter(MANGA_ID, chapterId)
                }
            }
        }

    private fun markerFile(pageFile: File): File = File(pageFile.parentFile, ".complete")

    private fun storage(): OfflineDownloadStorage =
        OfflineDownloadStorage(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    private fun pageBytes() = ByteArrayInputStream(byteArrayOf(1, 2, 3))

    private companion object {
        const val MANGA_ID = "storage-test-manga"
        const val CHAPTER_ID = "storage-test-chapter"
        const val LEGACY_CHAPTER_ID = "storage-test-legacy-chapter"
        const val RACE_ITERATIONS = 100
        const val CONCURRENT_WRITERS = 3
        const val BARRIER_TIMEOUT_SECONDS = 5L
    }
}
