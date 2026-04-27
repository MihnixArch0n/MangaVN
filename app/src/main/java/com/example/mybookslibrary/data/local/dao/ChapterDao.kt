package com.example.mybookslibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChapterDao {

    @Query("SELECT * FROM chapter_progress WHERE manga_id = :mangaId ORDER BY updated_at DESC")
    abstract fun getChapterProgressByManga(mangaId: String): Flow<List<ChapterProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertChapterProgress(progress: ChapterProgressEntity)

    @Query("DELETE FROM chapter_progress WHERE manga_id = :mangaId")
    protected abstract suspend fun deleteProgressByMangaId(mangaId: String)

    @Query("DELETE FROM library_items WHERE manga_id = :mangaId")
    protected abstract suspend fun deleteLibraryItemByMangaId(mangaId: String)

    @Transaction
    open suspend fun deleteLibraryItemAndProgress(mangaId: String) {
        deleteProgressByMangaId(mangaId)
        deleteLibraryItemByMangaId(mangaId)
    }
}

