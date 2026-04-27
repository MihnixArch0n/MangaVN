package com.example.mybookslibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mybookslibrary.data.local.LibraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<LibraryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items ORDER BY added_at DESC")
    fun getBookmarkedMangas(): Flow<List<LibraryItemEntity>>

    @Query("SELECT COUNT(*) FROM library_items")
    suspend fun count(): Int

    @Query("SELECT * FROM library_items WHERE manga_id = :mangaId LIMIT 1")
    suspend fun getByMangaId(mangaId: String): LibraryItemEntity?

    @Query("DELETE FROM library_items WHERE manga_id = :mangaId")
    suspend fun deleteByMangaId(mangaId: String)

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
