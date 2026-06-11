package com.example.mybookslibrary.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryDaoTest {
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
    fun upsert_thenGetByMangaId_returnsItem() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(item("manga-1", title = "One Piece"))

            assertEquals("One Piece", dao.getByMangaId("manga-1")?.title)
            assertNull(dao.getByMangaId("manga-missing"))
        }

    @Test
    fun upsertList_thenCount_returnsAllItems() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(listOf(item("manga-1"), item("manga-2"), item("manga-3")))

            assertEquals(3, dao.count())
        }

    @Test
    fun updateReadingProgress_updatesResumeColumnsAndReturnsAffectedCount() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(item("manga-1", updatedAt = 1_000L))

            val affected =
                dao.updateReadingProgress(
                    mangaId = "manga-1",
                    chapterId = "chapter-9",
                    pageIndex = 7,
                    updatedAt = 2_000L,
                )

            val updated = dao.getByMangaId("manga-1")
            assertEquals(1, affected)
            assertEquals("chapter-9", updated?.last_read_chapter_id)
            assertEquals(7, updated?.last_read_page_index)
            assertEquals(2_000L, updated?.updated_at)
        }

    @Test
    fun updateReadingProgress_returnsZeroWhenMangaNotInLibrary() =
        runTest {
            assertEquals(
                0,
                database.libraryDao().updateReadingProgress(
                    mangaId = "manga-missing",
                    chapterId = "chapter-1",
                    pageIndex = 0,
                    updatedAt = 1_000L,
                ),
            )
        }

    @Test
    fun updateReadingProgress_keepsChapterProgressRows() =
        runTest {
            // Regression cho pitfall trong KDoc của LibraryDao: REPLACE upsert là
            // delete-then-insert nên cascade-delete chapter_progress; UPDATE thì không.
            val dao = database.libraryDao()
            dao.upsert(item("manga-1"))
            database.chapterDao().upsertChapterProgress(progress("chapter-1", "manga-1"))

            dao.updateReadingProgress(
                mangaId = "manga-1",
                chapterId = "chapter-1",
                pageIndex = 3,
                updatedAt = 2_000L,
            )

            assertNotNull(database.chapterDao().getChapterProgressByChapter("chapter-1"))
        }

    @Test
    fun observeAll_ordersByUpdatedAtDescending() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(item("manga-old", updatedAt = 1_000L))
            dao.upsert(item("manga-new", updatedAt = 3_000L))
            dao.upsert(item("manga-mid", updatedAt = 2_000L))

            assertEquals(
                listOf("manga-new", "manga-mid", "manga-old"),
                dao.observeAll().first().map { it.manga_id },
            )
        }

    @Test
    fun deleteByMangaId_removesOnlyTargetItem() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(listOf(item("manga-1"), item("manga-2")))

            dao.deleteByMangaId("manga-1")

            assertNull(dao.getByMangaId("manga-1"))
            assertNotNull(dao.getByMangaId("manga-2"))
        }

    @Test
    fun deleteAll_emptiesLibrary() =
        runTest {
            val dao = database.libraryDao()
            dao.upsert(listOf(item("manga-1"), item("manga-2")))

            dao.deleteAll()

            assertEquals(0, dao.count())
        }

    private fun item(
        mangaId: String,
        title: String = "Title $mangaId",
        updatedAt: Long = 1_000L,
    ) = LibraryItemEntity(
        manga_id = mangaId,
        title = title,
        cover_url = "https://example.com/$mangaId.png",
        updated_at = updatedAt,
    )

    private fun progress(
        chapterId: String,
        mangaId: String,
    ) = ChapterProgressEntity(
        chapter_id = chapterId,
        manga_id = mangaId,
        updated_at = 1_000L,
    )
}
