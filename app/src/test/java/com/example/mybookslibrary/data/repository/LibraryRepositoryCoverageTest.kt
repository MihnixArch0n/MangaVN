@file:Suppress("ktlint")

package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phủ các thao tác thư viện của [LibraryRepository] với Room in-memory: thêm/xóa/kiểm tra
 * tồn tại, xóa toàn bộ, observe, và đổi trạng thái đọc của chapter.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryCoverageTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository =
            LibraryRepository(db.libraryDao(), db.chapterDao(), db, io.mockk.mockk(relaxed = true), io.mockk.mockk(relaxed = true), kotlinx.coroutines.test.TestScope())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addToLibrary_makesMangaPresentAndListed() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c", status = LibraryStatus.READING)

            assertTrue(repository.isInLibrary(MANGA_ID))
            assertEquals(listOf(MANGA_ID), repository.getAllItems().map { it.manga_id })
        }

    @Test
    fun removeFromLibrary_makesMangaAbsent() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            repository.removeFromLibrary(MANGA_ID)

            assertFalse(repository.isInLibrary(MANGA_ID))
        }

    @Test
    fun clearAll_emptiesLibrary() =
        runTest {
            repository.addToLibrary("m1", title = "A", coverUrl = "c")
            repository.addToLibrary("m2", title = "B", coverUrl = "c")

            repository.clearAll()

            assertTrue(repository.getAllItems().isEmpty())
        }

    @Test
    fun observeLibraryItems_emitsAddedItems() =
        runTest {
            repository.addToLibrary(MANGA_ID, title = "Manga", coverUrl = "c")

            val items = repository.observeLibraryItems().first()

            assertEquals(listOf(MANGA_ID), items.map { it.manga_id })
        }

    @Test
    fun markChapterUnread_setsUnreadStatusAndResetsPage() =
        runTest {
            db.libraryDao().upsert(LibraryItemEntity(manga_id = MANGA_ID, title = "T", cover_url = ""))
            db.chapterDao().upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = CHAPTER_ID,
                    manga_id = MANGA_ID,
                    status = ChapterStatus.COMPLETED,
                    last_read_page = 9,
                    total_pages = 10,
                    updated_at = 1L,
                ),
            )

            repository.markChapterUnread(MANGA_ID, CHAPTER_ID, totalPages = 10)

            val progress = db.chapterDao().getChapterProgressByChapter(CHAPTER_ID)!!
            assertEquals(ChapterStatus.UNREAD, progress.status)
            assertEquals(0, progress.last_read_page)
        }

    @Test
    fun removeBookmark_deletesLibraryItemAndProgress() =
        runTest {
            db.libraryDao().upsert(LibraryItemEntity(manga_id = MANGA_ID, title = "T", cover_url = ""))
            db.chapterDao().upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = CHAPTER_ID,
                    manga_id = MANGA_ID,
                    updated_at = 1L,
                ),
            )

            repository.removeBookmark(MANGA_ID)

            assertFalse(repository.isInLibrary(MANGA_ID))
            org.junit.Assert.assertNotNull(db.chapterDao().getChapterProgressByChapter(CHAPTER_ID))
        }

    @Test
    fun restoreItems_insertsAllItems() =
        runTest {
            val items =
                listOf(
                    LibraryItemEntity(manga_id = "m1", title = "T1", cover_url = ""),
                    LibraryItemEntity(manga_id = "m2", title = "T2", cover_url = ""),
                )

            repository.restoreItems(items)

            assertTrue(repository.isInLibrary("m1"))
            assertTrue(repository.isInLibrary("m2"))
        }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
