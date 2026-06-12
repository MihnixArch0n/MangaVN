package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.local.ChapterMetadataEntity
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * E2E download flow trên filesystem + Room + WorkManager THẬT (emulator):
 * ghi trang -> marker -> scan thấy -> query hiển thị -> xóa sạch (kể cả dir cha).
 * Đây là chuỗi từng chết im lặng nhiều tháng (màn Downloads rỗng vĩnh viễn)
 * vì không có test nào nối filesystem với UI data source.
 */
@HiltAndroidTest
class DownloadFlowTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var storage: OfflineDownloadStorage

    @Inject
    lateinit var downloadManager: OfflineDownloadManager

    @Inject
    lateinit var chapterDao: ChapterDao

    @Inject
    lateinit var libraryDao: LibraryDao

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown(): Unit =
        runBlocking {
            // Dọn filesystem + DB để không rò state sang test khác
            storage.deleteChapter(MANGA_ID, CHAPTER_ID)
            storage.deleteChapter(MANGA_ID, ORPHAN_CHAPTER_ID)
            chapterDao.deleteLibraryItemAndProgress(MANGA_ID)
        }

    private suspend fun writeCompletedChapter(chapterId: String, pages: Int = 2) {
        repeat(pages) { index ->
            storage.savePage(
                mangaId = MANGA_ID,
                chapterId = chapterId,
                pageIndex = index,
                byteStream = "page-$index".byteInputStream(),
                extension = "png",
            )
        }
        storage.markChapterComplete(MANGA_ID, chapterId, totalPages = pages)
    }

    @Test
    fun downloadHoanChinh_scanThayVaCoSize_partialKhongDuocTinh() =
        runBlocking {
            writeCompletedChapter(CHAPTER_ID)
            // Partial: có trang nhưng KHÔNG có completion marker
            storage.savePage(MANGA_ID, ORPHAN_CHAPTER_ID, 0, "partial".byteInputStream(), "png")

            val downloaded = storage.listDownloadedChapters()
            val entry = downloaded.firstOrNull { it.chapterId == CHAPTER_ID }

            assertEquals(MANGA_ID, entry?.mangaId)
            assertTrue("Size phải > 0", (entry?.sizeBytes ?: 0L) > 0L)
            assertFalse(
                "Partial (không marker) không được tính là đã tải",
                downloaded.any { it.chapterId == ORPHAN_CHAPTER_ID },
            )
        }

    @Test
    fun queryHienThi_leftJoinTraTitleChoMangaTrongLibrary_nullChoMangaNgoaiLibrary() =
        runBlocking {
            libraryDao.upsert(
                LibraryItemEntity(MANGA_ID, "One Piece", "cover", LibraryStatus.READING),
            )
            chapterDao.syncChapterMetadata(
                mangaId = MANGA_ID,
                chapters = listOf(
                    ChapterMetadataEntity(
                        chapterId = CHAPTER_ID,
                        mangaId = MANGA_ID,
                        volume = "1",
                        chapterNumber = "3",
                        title = null,
                        pages = 2,
                        isUnavailable = false,
                        translatedLanguage = "en",
                        feedOrder = 0,
                        updatedAt = 1L,
                    ),
                ),
                downloadedChapterIds = emptySet(),
            )

            val infos = chapterDao.getDownloadedChapterInfo(setOf(CHAPTER_ID))

            assertEquals(1, infos.size)
            assertEquals("One Piece", infos[0].mangaTitle)
            assertEquals("Vol. 1 Ch. 3", infos[0].buildChapterLabel())

            // Manga rời library -> LEFT JOIN vẫn trả row, title null (nhóm "Không còn trong thư viện")
            chapterDao.deleteLibraryItemAndProgress(MANGA_ID)
            val orphanInfos = chapterDao.getDownloadedChapterInfo(setOf(CHAPTER_ID))
            assertEquals(1, orphanInfos.size)
            assertNull(orphanInfos[0].mangaTitle)
        }

    @Test
    fun deleteDownload_xoaFileVaDirChaRong() =
        runBlocking {
            writeCompletedChapter(CHAPTER_ID)
            assertTrue(storage.listDownloadedChapters().any { it.chapterId == CHAPTER_ID })

            // Qua manager: cancel WorkManager (test mode) -> xóa file -> clear DB/cache
            downloadManager.deleteDownload(MANGA_ID, CHAPTER_ID)

            assertFalse(
                "File phải bị xóa sạch sau deleteDownload",
                storage.listDownloadedChapters().any { it.chapterId == CHAPTER_ID },
            )
            assertFalse(
                "Chapter đã xóa không còn được verify là downloaded",
                storage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID),
            )
        }

    private companion object {
        const val MANGA_ID = "download-flow-test-manga"
        const val CHAPTER_ID = "download-flow-test-chapter"
        const val ORPHAN_CHAPTER_ID = "download-flow-test-partial"
    }
}
