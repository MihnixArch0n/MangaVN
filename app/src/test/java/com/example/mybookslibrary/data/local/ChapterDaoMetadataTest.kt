package com.example.mybookslibrary.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChapterDaoMetadataTest {
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
    fun syncChapterMetadata_keepsDownloadedStaleChapterAndDeletesOtherStaleChapter() =
        runTest {
            val dao = database.chapterDao()
            dao.syncChapterMetadata(
                mangaId = MANGA_ID,
                chapters = listOf(metadata("chapter-1", 0), metadata("chapter-2", 1), metadata("chapter-3", 2)),
                downloadedChapterIds = emptySet(),
            )

            dao.syncChapterMetadata(
                mangaId = MANGA_ID,
                chapters = listOf(metadata("chapter-1", 0)),
                downloadedChapterIds = setOf("chapter-2"),
            )

            assertEquals(
                listOf("chapter-1", "chapter-2"),
                dao.getChaptersByMangaIdFlow(MANGA_ID).first().map { it.chapterId },
            )
        }

    @Test
    fun syncChapterMetadata_withEmptyFeed_deletesAllUndownloadedChapters() =
        runTest {
            val dao = database.chapterDao()
            dao.syncChapterMetadata(
                mangaId = MANGA_ID,
                chapters = listOf(metadata("chapter-1", 0), metadata("chapter-2", 1)),
                downloadedChapterIds = emptySet(),
            )

            // Feed rỗng (nhánh chapters.isEmpty) → xóa toàn bộ chapter không được download giữ lại
            dao.syncChapterMetadata(
                mangaId = MANGA_ID,
                chapters = emptyList(),
                downloadedChapterIds = setOf("chapter-2"),
            )

            assertEquals(
                listOf("chapter-2"),
                dao.getChaptersByMangaIdFlow(MANGA_ID).first().map { it.chapterId },
            )
        }

    private fun metadata(
        chapterId: String,
        feedOrder: Int,
    ) = ChapterMetadataEntity(
        chapterId = chapterId,
        mangaId = MANGA_ID,
        volume = null,
        chapterNumber = null,
        title = null,
        pages = 10,
        isUnavailable = false,
        feedOrder = feedOrder,
        updatedAt = 1_000L,
    )

    private companion object {
        const val MANGA_ID = "manga-1"
    }
}
