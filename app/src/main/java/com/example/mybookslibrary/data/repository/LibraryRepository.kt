package com.example.mybookslibrary.data.repository

import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.LibraryDao
import kotlinx.coroutines.flow.Flow

// Repository quản lý thư viện cá nhân (Room DB)
class LibraryRepository(
    private val libraryDao: LibraryDao
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
        libraryDao.upsert(
            LibraryItemEntity(
                manga_id = mangaId,
                title = title,
                cover_url = coverUrl,
                status = status,
                last_read_chapter_id = null,
                last_read_page_index = 0,
                updated_at = System.currentTimeMillis()
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
        pageIndex: Int
    ) {
        libraryDao.updateReadingProgress(
            mangaId = mangaId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            updatedAt = System.currentTimeMillis()
        )
    }
}
