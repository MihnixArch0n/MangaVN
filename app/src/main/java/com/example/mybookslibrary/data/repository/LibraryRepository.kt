package com.example.mybookslibrary.data.repository

import android.util.Log
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import com.example.mybookslibrary.data.local.dao.LibraryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class LibraryRepository(
    private val libraryDao: LibraryDao
) {
    companion object {
        private const val TAG = "LibraryRepository"
    }

    fun observeLibraryItems(): Flow<List<LibraryItemEntity>> = libraryDao.observeAll()

    /**
     * Mock data flow dùng để test UI trước khi build đầy đủ tính năng.
     */
    fun mockLibraryItemsFlow(): Flow<List<LibraryItemEntity>> {
        val now = System.currentTimeMillis()
        val items = listOf(
            LibraryItemEntity(
                manga_id = "manga_test_001",
                title = "Test Manga: API Reading Demo",
                cover_url = "https://example.com/cover_test.jpg",
                status = LibraryStatus.READING,
                // REPLACE_WITH_REAL_CHAPTER_ID: Get a real chapter UUID from MangaDex
                // Example: Go to https://mangadex.org/chapter/{chapter_id}
                last_read_chapter_id = "ed39bc37-2d3d-40c7-9bdd-bec6865756d7",
                last_read_page_index = 0,
                updated_at = now
            ),
            LibraryItemEntity(
                manga_id = "manga_fst_001",
                title = "Fake Manga One",
                cover_url = "https://example.com/cover_one.jpg",
                status = LibraryStatus.READING,
                last_read_chapter_id = "ch_010",
                last_read_page_index = 2,
                updated_at = now - 3_600_000L
            ),
            LibraryItemEntity(
                manga_id = "manga_fst_002",
                title = "Fake Manga Two",
                cover_url = "https://example.com/cover_two.jpg",
                status = LibraryStatus.FAVORITE,
                last_read_chapter_id = "ch_000",
                last_read_page_index = 0,
                updated_at = now - 7_200_000L
            ),
            LibraryItemEntity(
                manga_id = "manga_fst_003",
                title = "Fake Manga Three",
                cover_url = "https://example.com/cover_three.jpg",
                status = LibraryStatus.READING,
                last_read_chapter_id = "ch_002",
                last_read_page_index = 5,
                updated_at = now - 10_800_000L
            )
        )
        return flowOf(items)
    }

    /**
     * Seed dữ liệu giả vào Room DB nếu database đang trống.
     */
    suspend fun seedMockIfEmpty() {
        try {
            val currentCount = libraryDao.count()
            Log.d(TAG, "seedMockIfEmpty: Current count = $currentCount")

            if (currentCount > 0) {
                Log.d(TAG, "seedMockIfEmpty: Database không trống, bỏ qua seed")
                return
            }

            val items = mockLibraryItemsFlow().first()
            Log.d(TAG, "seedMockIfEmpty: Seeding ${items.size} mock items")

            libraryDao.upsert(items)

            val newCount = libraryDao.count()
            Log.d(TAG, "seedMockIfEmpty: Seed thành công! Mới có $newCount items")
        } catch (e: Exception) {
            Log.e(TAG, "seedMockIfEmpty: Error", e)
        }
    }

    /**
     * DEBUG: Force-clear database và reseed (dùng cho testing).
     */
    suspend fun debugClearAndReseed() {
        try {
            Log.d(TAG, "debugClearAndReseed: Clearing all items...")
            libraryDao.deleteAll()

            val items = mockLibraryItemsFlow().first()
            Log.d(TAG, "debugClearAndReseed: Reseeding ${items.size} items...")
            libraryDao.upsert(items)

            Log.d(TAG, "debugClearAndReseed: Done! Database now has ${libraryDao.count()} items")
        } catch (e: Exception) {
            Log.e(TAG, "debugClearAndReseed: Error", e)
        }
    }

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
