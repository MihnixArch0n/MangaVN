package com.example.mybookslibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mybookslibrary.data.local.LibraryItemEntity
import com.example.mybookslibrary.data.local.LibraryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<LibraryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE manga_id = :mangaId")
    suspend fun deleteByMangaId(mangaId: String)

    @Query("SELECT * FROM library_items ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE status = :status ORDER BY updated_at DESC")
    fun observeByStatus(status: LibraryStatus): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items ORDER BY updated_at DESC")
    suspend fun getAll(): List<LibraryItemEntity>

    @Query("SELECT COUNT(*) FROM library_items")
    suspend fun count(): Int

    @Query("SELECT * FROM library_items WHERE manga_id = :mangaId LIMIT 1")
    suspend fun getByMangaId(mangaId: String): LibraryItemEntity?

    @Query(
        """
        UPDATE library_items
        SET last_read_chapter_id = :chapterId,
            last_read_page_index = :pageIndex,
            updated_at = :updatedAt
        WHERE manga_id = :mangaId
        """
    )
    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        pageIndex: Int,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
