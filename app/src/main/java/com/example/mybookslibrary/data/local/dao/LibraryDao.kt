package com.example.mybookslibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.mybookslibrary.data.local.LibraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Upsert
    suspend fun upsert(items: List<LibraryItemEntity>)

    @Upsert
    suspend fun upsert(item: LibraryItemEntity)

    /**
     * Updates only the resume-reading columns for an existing library item.
     *
     * Do not use the REPLACE upsert path for this write: SQLite implements REPLACE
     * as delete-then-insert, which can cascade-delete chapter_progress rows.
     */
    @Query(
        """
        UPDATE library_items
        SET last_read_chapter_id = :chapterId,
            last_read_page_index = :pageIndex,
            updated_at = :updatedAt,
            sync_status = 'PENDING_UPDATE'
        WHERE manga_id = :mangaId
        """,
    )
    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        pageIndex: Int,
        updatedAt: Long,
    ): Int

    @Query("UPDATE library_items SET sync_status = 'PENDING_DELETE' WHERE manga_id = :mangaId")
    suspend fun markDeleted(mangaId: String)

    @Query("DELETE FROM library_items WHERE manga_id = :mangaId")
    suspend fun physicallyDelete(mangaId: String)

    @Query("SELECT * FROM library_items WHERE sync_status != 'PENDING_DELETE' ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE sync_status != 'PENDING_DELETE' ORDER BY updated_at DESC")
    suspend fun getAll(): List<LibraryItemEntity>

    @Query("SELECT COUNT(*) FROM library_items WHERE sync_status != 'PENDING_DELETE'")
    suspend fun count(): Int

    @Query("SELECT * FROM library_items WHERE manga_id = :mangaId AND sync_status != 'PENDING_DELETE' LIMIT 1")
    suspend fun getByMangaId(mangaId: String): LibraryItemEntity?

    @Query("SELECT * FROM library_items WHERE sync_status IN ('PENDING_UPDATE', 'PENDING_DELETE')")
    suspend fun getPendingSyncItems(): List<LibraryItemEntity>

    @Query("UPDATE library_items SET sync_status = 'SYNCED' WHERE manga_id = :mangaId")
    suspend fun markSynced(mangaId: String)

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
