@file:Suppress("ktlint")

package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Regression guard cho finding W8: `restoreItems` phải cập nhật tại chỗ (@Upsert) thay vì
 * REPLACE (delete-then-insert) cho library_items, vì FK onDelete=CASCADE sẽ xóa toàn bộ
 * chapter_progress khi restore đè một manga đã có trong thư viện.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryRestoreTest {
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
    fun restoreItems_existingManga_preservesChapterProgress() =
        runTest {
            val mangaId = "manga-1"
            db.libraryDao().upsert(LibraryItemEntity(manga_id = mangaId, title = "T", cover_url = ""))
            db.chapterDao().upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = "chapter-1",
                    manga_id = mangaId,
                    status = ChapterStatus.READING,
                    last_read_page = 3,
                    total_pages = 10,
                    updated_at = 1_000L,
                    is_downloaded = true,
                ),
            )

            // Restore backup chứa manga đã có trong thư viện
            repository.restoreItems(
                listOf(LibraryItemEntity(manga_id = mangaId, title = "T mới", cover_url = "")),
            )

            // chapter_progress KHÔNG được bị cascade xóa
            val progress = db.chapterDao().getChapterProgressByChapter("chapter-1")
            assertNotNull("restoreItems không được xóa chapter_progress của manga đã có", progress)
        }
}
