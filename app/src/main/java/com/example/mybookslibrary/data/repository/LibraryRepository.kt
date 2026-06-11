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
import com.example.mybookslibrary.data.remote.models.FirestoreChapterProgress
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

        uploadPendingItems(user.uid)
        try {
            mergeLibraryItems(user.uid)
            syncChapterProgress(user.uid)
        } catch (e: Exception) {
            Timber.e(e, "Error downloading and merging items from Firestore")
            throw e
        }
    }

    private suspend fun uploadPendingItems(userId: String) {
        val pendingItems = libraryDao.getPendingSyncItems()
        for (item in pendingItems) {
            try {
                if (item.syncStatus == SyncStatus.PENDING_DELETE) {
                    firestoreDataSource.deleteItem(userId, item.manga_id)
                    libraryDao.physicallyDelete(item.manga_id)
                    chapterDao.deleteLibraryItemAndProgress(item.manga_id)
                } else {
                    firestoreDataSource.saveItem(userId, item.toFirestore())
                    libraryDao.markSynced(item.manga_id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during uploading pending item ${item.manga_id}")
            }
        }
    }

    private suspend fun mergeLibraryItems(userId: String) {
        val remoteItems = firestoreDataSource.getAllItems(userId)
        val localItems = libraryDao.getAll()
        val localItemsById = localItems.associateBy { it.manga_id }
        val remoteItemIds = remoteItems.mapTo(mutableSetOf()) { it.mangaId }

        val itemsToUpsert =
            remoteItems.mapNotNull { remoteItem ->
                val localItem = localItemsById[remoteItem.mangaId]
                when {
                    localItem == null -> remoteItem.toLocal()
                    remoteItem.updatedAt > localItem.updated_at -> remoteItem.mergeInto(localItem)
                    else -> null
                }
            }

        if (itemsToUpsert.isNotEmpty()) {
            libraryDao.upsert(itemsToUpsert)
        }

        localItems
            .filter { it.syncStatus == SyncStatus.SYNCED && it.manga_id !in remoteItemIds }
            .forEach { item ->
                libraryDao.physicallyDelete(item.manga_id)
                chapterDao.deleteLibraryItemAndProgress(item.manga_id)
            }
    }

    private suspend fun syncChapterProgress(userId: String) {
        val remoteProgress = firestoreDataSource.getAllProgress(userId)
        val localProgress = chapterDao.getAllProgress()
        val remoteByChapterId = remoteProgress.associateBy { it.chapterId }
        val localByChapterId = localProgress.associateBy { it.chapter_id }

        val progressToUpload =
            localProgress
                .filter { local ->
                    val remote = remoteByChapterId[local.chapter_id]
                    remote == null || local.updated_at > remote.updatedAt
                }.map { it.toFirestore() }
        if (progressToUpload.isNotEmpty()) {
            firestoreDataSource.saveProgressList(userId, progressToUpload)
        }

        remoteProgress
            .filter { remote ->
                val local = localByChapterId[remote.chapterId]
                local == null || remote.updatedAt > local.updated_at
            }.filter { libraryDao.getByMangaId(it.mangaId) != null }
            .map { remote -> remote.toLocal(localByChapterId[remote.chapterId]) }
            .forEach { chapterDao.upsertChapterProgress(it) }
    }

    private fun LibraryItemEntity.toFirestore() =
        FirestoreLibraryItem(
            mangaId = manga_id,
            title = title,
            coverUrl = cover_url,
            status = status.name,
            addedAt = updated_at,
            lastReadAt = updated_at,
            lastChapterId = last_read_chapter_id,
            updatedAt = updated_at,
        )

    private fun FirestoreLibraryItem.toLocal() =
        LibraryItemEntity(
            manga_id = mangaId,
            title = title,
            cover_url = coverUrl ?: "",
            status = LibraryStatus.entries.firstOrNull { it.name == status } ?: LibraryStatus.READING,
            last_read_chapter_id = lastChapterId,
            last_read_page_index = 0,
            updated_at = updatedAt,
            syncStatus = SyncStatus.SYNCED,
        )

    private fun FirestoreLibraryItem.mergeInto(local: LibraryItemEntity) =
        local.copy(
            title = title,
            cover_url = coverUrl ?: "",
            status = LibraryStatus.entries.firstOrNull { it.name == status } ?: LibraryStatus.READING,
            last_read_chapter_id = lastChapterId,
            updated_at = updatedAt,
            syncStatus = SyncStatus.SYNCED,
        )

    private fun ChapterProgressEntity.toFirestore() =
        FirestoreChapterProgress(
            chapterId = chapter_id,
            mangaId = manga_id,
            status = status.name,
            lastReadPage = last_read_page,
            totalPages = total_pages,
            updatedAt = updated_at,
        )

    private fun FirestoreChapterProgress.toLocal(local: ChapterProgressEntity?) =
        ChapterProgressEntity(
            chapter_id = chapterId,
            manga_id = mangaId,
            status = ChapterStatus.entries.firstOrNull { it.name == status } ?: ChapterStatus.UNREAD,
            last_read_page = lastReadPage,
            total_pages = totalPages,
            updated_at = updatedAt,
            is_downloaded = local?.is_downloaded ?: false,
        )
}
