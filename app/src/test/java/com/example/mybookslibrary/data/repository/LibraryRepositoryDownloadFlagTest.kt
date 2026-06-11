@file:Suppress("ktlint")

package com.example.mybookslibrary.data.repository

import androidx.room.Room
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.dao.ChapterDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Regression guard cho finding H4/W9: `markChapterCompleted` / `markChapterUnread`
 * phải GIỮ cờ `is_downloaded` của chapter đã tải (cập nhật tại chỗ thay vì
 * delete-then-insert). Dùng Room in-memory thật để kiểm tra hành vi persistence.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryDownloadFlagTest {
    private lateinit var db: AppDatabase
    private lateinit var chapterDao: ChapterDao
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
        chapterDao = db.chapterDao()
        repository =
            LibraryRepository(db.libraryDao(), chapterDao, db, io.mockk.mockk(relaxed = true), io.mockk.mockk(relaxed = true), kotlinx.coroutines.test.TestScope())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun markChapterCompleted_preservesIsDownloadedFlag() =
        runTest {
            val mangaId = "manga-1"
            val chapterId = "chapter-1"
            // Manga cha (vì FK), rồi chapter đã tải về (is_downloaded = true).
            db.libraryDao().upsert(LibraryItemEntity(manga_id = mangaId, title = "T", cover_url = ""))
            chapterDao.upsertChapterProgress(
                ChapterProgressEntity(
                    chapter_id = chapterId,
                    manga_id = mangaId,
                    status = ChapterStatus.READING,
                    last_read_page = 3,
                    total_pages = 10,
                    updated_at = 1_000L,
                    is_downloaded = true,
                ),
            )

            // Đánh dấu đã đọc xong — KHÔNG được làm mất trạng thái đã tải.
            repository.markChapterCompleted(mangaId, chapterId, totalPages = 10)

            val after = chapterDao.getChapterProgressByChapter(chapterId)
            assertTrue(
                "markChapterCompleted không được reset is_downloaded của chapter đã tải",
                after?.is_downloaded == true,
            )
        }
}
