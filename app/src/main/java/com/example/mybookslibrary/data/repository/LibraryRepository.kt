package com.example.mybookslibrary.data.repository

import androidx.room.withTransaction
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.LibraryDao
import com.example.mybookslibrary.data.remote.FirestoreDataSource
import com.example.mybookslibrary.data.remote.models.FirestoreLibraryItem
import com.example.mybookslibrary.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

// Repository quản lý thư viện cá nhân (Room DB + Firestore Sync)
class LibraryRepository(
    private val libraryDao: LibraryDao,
    private val chapterDao: ChapterDao,
    private val database: AppDatabase,
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository,
    private val externalScope: CoroutineScope,
) {
    /** Reactive stream danh sách manga trong thư viện, dùng cho [LibraryScreen]. */
    fun observeLibraryItems(): Flow<List<LibraryItemEntity>> = libraryDao.observeAll()

    /** Lấy toàn bộ items một lần, dùng cho backup. */
    suspend fun getAllItems(): List<LibraryItemEntity> = libraryDao.getAll()

    /** Thêm hoặc cập nhật manga trong thư viện. Mặc định trạng thái [LibraryStatus.READING]. */
    suspend fun addToLibrary(
        mangaId: String,
        title: String,
        coverUrl: String,
        status: LibraryStatus = LibraryStatus.READING,
    ) {
        val now = System.currentTimeMillis()
        val entity = LibraryItemEntity(
            manga_id = mangaId,
            title = title,
            cover_url = coverUrl,
            status = status,
            last_read_chapter_id = null,
            last_read_page_index = 0,
            updated_at = now,
            sync_status = SyncStatus.PENDING_UPDATE
        )
        libraryDao.upsert(entity)
        trySyncItem(entity)
    }

    /** Xóa manga khỏi thư viện (đánh dấu PENDING_DELETE thay vì xóa ngay để sync worker còn bắt được). */
    suspend fun removeFromLibrary(mangaId: String) {
        libraryDao.markDeleted(mangaId)
        val user = authRepository.getCurrentUser()
        if (user != null) {
            externalScope.launch {
                try {
                    firestoreDataSource.deleteItem(user.uid, mangaId)
                    libraryDao.physicallyDelete(mangaId)
                    chapterDao.deleteLibraryItemAndProgress(mangaId)
                } catch (e: Exception) {
                    Timber.e(e, "Error deleting Firestore item")
                }
            }
        } else {
            // Không đăng nhập -> xóa ngay
            libraryDao.physicallyDelete(mangaId)
            chapterDao.deleteLibraryItemAndProgress(mangaId)
        }
    }

    /** Kiểm tra manga đã có trong thư viện chưa. */
    suspend fun isInLibrary(mangaId: String): Boolean = libraryDao.getByMangaId(mangaId) != null

    /** Xóa toàn bộ thư viện. Gọi khi sign out. */
    suspend fun clearAll() {
        libraryDao.deleteAll()
    }

    /** Xóa toàn bộ dữ liệu trên Firestore (dùng khi xóa tài khoản). */
    suspend fun clearAllRemote() {
        val user = authRepository.getCurrentUser() ?: return
        firestoreDataSource.deleteAllUserData(user.uid)
    }

    /** Upsert danh sách items từ backup JSON. */
    suspend fun restoreItems(items: List<LibraryItemEntity>) {
        libraryDao.upsert(items.map { it.copy(sync_status = SyncStatus.PENDING_UPDATE) })
        items.forEach { trySyncItem(it) }
    }

    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        pageIndex: Int,
        totalPages: Int,
    ) {
        val now = System.currentTimeMillis()
        val boundedTotalPages = totalPages.coerceAtLeast(0)
        val boundedPageIndex = pageIndex.coerceAtLeast(0)
        val isCompleted = boundedTotalPages > 0 && boundedPageIndex == (boundedTotalPages - 1)

        database.withTransaction {
            libraryDao.updateReadingProgress(
                mangaId = mangaId,
                chapterId = chapterId,
                pageIndex = boundedPageIndex,
                updatedAt = now,
            )

            chapterDao.upsertReadingProgress(
                ChapterProgressEntity(
                    chapter_id = chapterId,
                    manga_id = mangaId,
                    status = if (isCompleted) ChapterStatus.COMPLETED else ChapterStatus.READING,
                    last_read_page = boundedPageIndex,
                    total_pages = boundedTotalPages,
                    updated_at = now,
                ),
            )
        }
        
        val item = libraryDao.getByMangaId(mangaId)
        if (item != null) trySyncItem(item)
    }

    suspend fun markChapterCompleted(
        mangaId: String,
        chapterId: String,
        totalPages: Int,
    ) {
        val boundedTotalPages = totalPages.coerceAtLeast(0)
        chapterDao.upsertReadingProgress(
            ChapterProgressEntity(
                chapter_id = chapterId,
                manga_id = mangaId,
                status = ChapterStatus.COMPLETED,
                last_read_page = boundedTotalPages,
                total_pages = boundedTotalPages,
                updated_at = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markChapterUnread(
        mangaId: String,
        chapterId: String,
        totalPages: Int,
    ) {
        chapterDao.upsertReadingProgress(
            ChapterProgressEntity(
                chapter_id = chapterId,
                manga_id = mangaId,
                status = ChapterStatus.UNREAD,
                last_read_page = 0,
                total_pages = totalPages.coerceAtLeast(0),
                updated_at = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeBookmark(mangaId: String) {
        removeFromLibrary(mangaId)
    }

    private fun trySyncItem(item: LibraryItemEntity) {
        val user = authRepository.getCurrentUser() ?: return
        externalScope.launch {
            try {
                val firestoreItem = FirestoreLibraryItem(
                    mangaId = item.manga_id,
                    title = item.title,
                    coverUrl = item.cover_url,
                    status = item.status.name,
                    addedAt = item.updated_at,
                    lastReadAt = item.updated_at,
                    lastChapterId = item.last_read_chapter_id,
                    updatedAt = item.updated_at
                )
                firestoreDataSource.saveItem(user.uid, firestoreItem)
                libraryDao.markSynced(item.manga_id)
            } catch (e: Exception) {
                Timber.e(e, "Error syncing Firestore item")
            }
        }
    }

    // Dùng cho SyncWorker
    suspend fun syncPendingItems() {
        val user = authRepository.getCurrentUser() ?: return
        val pendingItems = libraryDao.getPendingSyncItems()
        for (item in pendingItems) {
            try {
                if (item.sync_status == SyncStatus.PENDING_DELETE) {
                    firestoreDataSource.deleteItem(user.uid, item.manga_id)
                    libraryDao.physicallyDelete(item.manga_id)
                    chapterDao.deleteLibraryItemAndProgress(item.manga_id)
                } else {
                    val firestoreItem = FirestoreLibraryItem(
                        mangaId = item.manga_id,
                        title = item.title,
                        coverUrl = item.cover_url,
                        status = item.status.name,
                        addedAt = item.updated_at,
                        lastReadAt = item.updated_at,
                        lastChapterId = item.last_read_chapter_id,
                        updatedAt = item.updated_at
                    )
                    firestoreDataSource.saveItem(user.uid, firestoreItem)
                    libraryDao.markSynced(item.manga_id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during SyncWorker for item ${item.manga_id}")
            }
        }
    }
}
