package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import kotlinx.coroutines.flow.Flow

// Repository quản lý thư viện cá nhân (Room DB)
class LibraryRepository(
    private val libraryDao: LibraryDao,
    private val chapterDao: ChapterDao
) {
    // Theo dõi realtime danh sách manga trong thư viện (dùng cho LibraryScreen)
    fun observeLibraryItems(): Flow<List<LibraryItemEntity>> = libraryDao.observeAll()

    // Lấy toàn bộ items (dùng cho backup)
    suspend fun getAllItems(): List<LibraryItemEntity> = libraryDao.getAll()

    // Thêm manga vào thư viện với trạng thái mặc định READING
    suspend fun addToLibrary(
        mangaId: String,
        title: String,
        coverUrl: String,
        status: LibraryStatus = LibraryStatus.READING
    ) {
        val now = System.currentTimeMillis()
        libraryDao.upsert(
            LibraryItemEntity(
                manga_id = mangaId,
                title = title,
                cover_url = coverUrl,
                status = status,
                last_read_chapter_id = null,
                last_read_page_index = 0,
                updated_at = now
            )
        )
    }

    suspend fun removeFromLibrary(mangaId: String) {
        libraryDao.deleteByMangaId(mangaId)
    }

    suspend fun isInLibrary(mangaId: String): Boolean {
        return libraryDao.getByMangaId(mangaId) != null
    }

    // Xóa toàn bộ thư viện (dùng cho sign out)
    suspend fun clearAll() {
        libraryDao.deleteAll()
    }

    // Khôi phục dữ liệu từ backup JSON
    suspend fun restoreItems(items: List<LibraryItemEntity>) {
        libraryDao.upsert(items)
    }

    // Cập nhật tiến độ đọc (chapter + page) — gọi từ ReaderViewModel mỗi khi chuyển trang
    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        pageIndex: Int,
        totalPages: Int
    ) {
        val now = System.currentTimeMillis()
        val boundedTotalPages = totalPages.coerceAtLeast(0)
        val boundedPageIndex = pageIndex.coerceAtLeast(0)
        val isCompleted = boundedTotalPages > 0 && boundedPageIndex >= (boundedTotalPages - 1)

        libraryDao.getByMangaId(mangaId)?.let { current ->
            libraryDao.upsert(
                current.copy(
                    last_read_chapter_id = chapterId,
                    last_read_page_index = boundedPageIndex,
                    updated_at = now
                )
            )
        }

        chapterDao.upsertChapterProgress(
            ChapterProgressEntity(
                chapter_id = chapterId,
                manga_id = mangaId,
                status = if (isCompleted) ChapterStatus.COMPLETED else ChapterStatus.READING,
                last_read_page = if (isCompleted) boundedTotalPages else boundedPageIndex,
                total_pages = boundedTotalPages,
                updated_at = now
            )
        )
    }

    suspend fun markChapterCompleted(
        mangaId: String,
        chapterId: String,
        totalPages: Int
    ) {
        val boundedTotalPages = totalPages.coerceAtLeast(0)
        chapterDao.upsertChapterProgress(
            ChapterProgressEntity(
                chapter_id = chapterId,
                manga_id = mangaId,
                status = ChapterStatus.COMPLETED,
                last_read_page = boundedTotalPages,
                total_pages = boundedTotalPages,
                updated_at = System.currentTimeMillis()
            )
        )
    }

    suspend fun markChapterUnread(
        mangaId: String,
        chapterId: String,
        totalPages: Int
    ) {
        chapterDao.upsertChapterProgress(
            ChapterProgressEntity(
                chapter_id = chapterId,
                manga_id = mangaId,
                status = ChapterStatus.UNREAD,
                last_read_page = 0,
                total_pages = totalPages.coerceAtLeast(0),
                updated_at = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeBookmark(mangaId: String) {
        chapterDao.deleteLibraryItemAndProgress(mangaId)
    }
}
