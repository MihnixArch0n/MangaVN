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
            syncStatus = SyncStatus.PENDING_UPDATE
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

    /** Lấy LibraryItemEntity theo mangaId. */
    suspend fun getLibraryItem(mangaId: String): LibraryItemEntity? = libraryDao.getByMangaId(mangaId)

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
        libraryDao.upsert(items.map { it.copy(syncStatus = SyncStatus.PENDING_UPDATE) })
        items.forEach { trySyncItem(it) }
    }

    /**
     * Updates the local reading progress of a page in a chapter, updates both Room DB tables inside a transaction,
     * and attempts to sync the changes to Firestore.
     *
     * @param mangaId The ID of the manga.
     * @param chapterId The ID of the chapter.
     * @param pageIndex The 0-based index of the read page.
     * @param totalPages The total number of pages in the chapter.
     */
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

    /**
     * Marks the given chapter as fully read (COMPLETED) in the database.
     *
     * @param mangaId The ID of the manga.
     * @param chapterId The ID of the chapter.
     * @param totalPages The total number of pages in the chapter.
     */
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

    /**
     * Marks the given chapter as UNREAD in the database, resetting its read page index to 0.
     *
     * @param mangaId The ID of the manga.
     * @param chapterId The ID of the chapter.
     * @param totalPages The total number of pages in the chapter.
     */
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

    /**
     * Removes bookmark for the manga, triggering offline updates and Firestore deletion tasks.
     *
     * @param mangaId The ID of the manga to be removed.
     */
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

    /**
     * Performs a full 2-way sync with Firestore.
     * 1. Uploads pending local changes.
     * 2. Downloads all remote items and merges them into the local database.
     */
    suspend fun performSync() {
        val user = authRepository.getCurrentUser() ?: return
        
        // 1. Upload pending changes from local first
        val pendingItems = libraryDao.getPendingSyncItems()
        for (item in pendingItems) {
            try {
                if (item.syncStatus == SyncStatus.PENDING_DELETE) {
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
                Timber.e(e, "Error during uploading pending item ${item.manga_id}")
            }
        }

        // 2. Download all items from cloud and merge to local
        try {
            val remoteItems = firestoreDataSource.getAllItems(user.uid)
            val remoteItemsMap = remoteItems.associateBy { it.mangaId }
            val localItems = libraryDao.getAll()
            
            val itemsToUpsertLocal = mutableListOf<LibraryItemEntity>()
            val itemsToDeleteLocal = mutableListOf<String>()
            
            // Handle remote items -> merge to local
            for (remoteItem in remoteItems) {
                val localItem = localItems.find { it.manga_id == remoteItem.mangaId }
                if (localItem == null) {
                    // Cloud only -> copy to local
                    itemsToUpsertLocal.add(
                        LibraryItemEntity(
                            manga_id = remoteItem.mangaId,
                            title = remoteItem.title,
                            cover_url = remoteItem.coverUrl ?: "",
                            status = try { LibraryStatus.valueOf(remoteItem.status) } catch(e: Exception) { LibraryStatus.READING },
                            last_read_chapter_id = remoteItem.lastChapterId,
                            last_read_page_index = 0, 
                            updated_at = remoteItem.updatedAt,
                            syncStatus = SyncStatus.SYNCED
                        )
                    )
                } else {
                    // Exists in both -> Last Write Wins
                    if (remoteItem.updatedAt > localItem.updated_at) {
                        itemsToUpsertLocal.add(
                            localItem.copy(
                                title = remoteItem.title,
                                cover_url = remoteItem.coverUrl ?: "",
                                status = try { LibraryStatus.valueOf(remoteItem.status) } catch(e: Exception) { LibraryStatus.READING },
                                last_read_chapter_id = remoteItem.lastChapterId,
                                updated_at = remoteItem.updatedAt,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    }
                }
            }
            
            // Handle local items missing on remote
            for (localItem in localItems) {
                if (!remoteItemsMap.containsKey(localItem.manga_id)) {
                    // Local only
                    if (localItem.syncStatus == SyncStatus.SYNCED) {
                        // It was synced before, but now missing remotely -> it was deleted on another device
                        itemsToDeleteLocal.add(localItem.manga_id)
                    }
                }
            }
            
            if (itemsToUpsertLocal.isNotEmpty()) {
                libraryDao.upsert(itemsToUpsertLocal)
            }
            
            for (mangaId in itemsToDeleteLocal) {
                libraryDao.physicallyDelete(mangaId)
                chapterDao.deleteLibraryItemAndProgress(mangaId)
            }

            // --- 3. Sync Chapter Progress ---
            val remoteProgress = firestoreDataSource.getAllProgress(user.uid)
            val remoteProgressMap = remoteProgress.associateBy { it.chapterId }
            val localProgress = chapterDao.getAllProgress()
            val localProgressMap = localProgress.associateBy { it.chapter_id }
            
            val progressToUpload = mutableListOf<com.example.mybookslibrary.data.remote.models.FirestoreChapterProgress>()
            val progressToUpsertLocal = mutableListOf<ChapterProgressEntity>()
            
            // Upload local progress that is newer or missing on remote
            for (local in localProgress) {
                val remote = remoteProgressMap[local.chapter_id]
                if (remote == null || local.updated_at > remote.updatedAt) {
                    progressToUpload.add(
                        com.example.mybookslibrary.data.remote.models.FirestoreChapterProgress(
                            chapterId = local.chapter_id,
                            mangaId = local.manga_id,
                            status = local.status.name,
                            lastReadPage = local.last_read_page,
                            totalPages = local.total_pages,
                            updatedAt = local.updated_at
                        )
                    )
                }
            }
            
            // Download remote progress that is newer or missing locally
            for (remote in remoteProgress) {
                val local = localProgressMap[remote.chapterId]
                if (local == null || remote.updatedAt > local.updated_at) {
                    // Make sure the manga still exists locally before inserting to avoid FK constraint errors
                    if (libraryDao.getByMangaId(remote.mangaId) != null) {
                        progressToUpsertLocal.add(
                            ChapterProgressEntity(
                                chapter_id = remote.chapterId,
                                manga_id = remote.mangaId,
                                status = try { ChapterStatus.valueOf(remote.status) } catch(e: Exception) { ChapterStatus.UNREAD },
                                last_read_page = remote.lastReadPage,
                                total_pages = remote.totalPages,
                                updated_at = remote.updatedAt,
                                is_downloaded = local?.is_downloaded ?: false
                            )
                        )
                    }
                }
            }
            
            if (progressToUpload.isNotEmpty()) {
                firestoreDataSource.saveProgressList(user.uid, progressToUpload)
            }
            for (progress in progressToUpsertLocal) {
                chapterDao.upsertChapterProgress(progress)
            }
            
        } catch (e: Exception) {
             Timber.e(e, "Error downloading and merging items from Firestore")
             throw e // Rethrow to let callers know it failed
        }
    }
}
