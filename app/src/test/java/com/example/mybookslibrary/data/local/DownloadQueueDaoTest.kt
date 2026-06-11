package com.example.mybookslibrary.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadQueueDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_thenGetQueueByChapter_returnsEntry() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-1", "manga-1"))

            assertEquals(DownloadStatus.PENDING, dao.getQueueByChapter("chapter-1")?.status)
            assertNull(dao.getQueueByChapter("chapter-missing"))
        }

    @Test
    fun updateStatus_updatesStatusProgressAndError() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-1", "manga-1"))

            dao.updateStatus(
                chapterId = "chapter-1",
                status = DownloadStatus.ERROR,
                progressPercent = 40,
                errorMessage = "Mạng bị gián đoạn",
            )

            val updated = dao.getQueueByChapter("chapter-1")
            assertEquals(DownloadStatus.ERROR, updated?.status)
            assertEquals(40, updated?.progress_percent)
            assertEquals("Mạng bị gián đoạn", updated?.error_msg)
        }

    @Test
    fun observeQueue_ordersByMangaIdThenChapterId() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-b", "manga-2"))
            dao.upsert(entry("chapter-a", "manga-2"))
            dao.upsert(entry("chapter-z", "manga-1"))

            assertEquals(
                listOf("chapter-z", "chapter-a", "chapter-b"),
                dao.observeQueue().first().map { it.chapter_id },
            )
        }

    @Test
    fun observeQueueByManga_filtersToSingleManga() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-1", "manga-1"))
            dao.upsert(entry("chapter-2", "manga-2"))

            assertEquals(
                listOf("chapter-1"),
                dao.observeQueueByManga("manga-1").first().map { it.chapter_id },
            )
        }

    @Test
    fun observeQueueByChapter_emitsNullAfterDelete() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-1", "manga-1"))

            dao.deleteByChapter("chapter-1")

            assertNull(dao.observeQueueByChapter("chapter-1").first())
        }

    @Test
    fun deleteByManga_removesAllChaptersOfManga() =
        runTest {
            val dao = database.downloadQueueDao()
            dao.upsert(entry("chapter-1", "manga-1"))
            dao.upsert(entry("chapter-2", "manga-1"))
            dao.upsert(entry("chapter-3", "manga-2"))

            dao.deleteByManga("manga-1")

            assertEquals(listOf("chapter-3"), dao.observeQueue().first().map { it.chapter_id })
        }

    private fun entry(
        chapterId: String,
        mangaId: String,
    ) = DownloadQueueEntity(
        chapter_id = chapterId,
        manga_id = mangaId,
    )
}
