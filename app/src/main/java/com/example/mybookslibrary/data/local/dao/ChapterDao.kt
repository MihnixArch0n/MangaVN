package com.example.mybookslibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.mybookslibrary.data.local.ChapterMetadataEntity
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChapterDao {
    @Query("SELECT * FROM chapter_metadata WHERE manga_id = :mangaId ORDER BY feed_order ASC")
    abstract fun getChaptersByMangaIdFlow(mangaId: String): Flow<List<ChapterMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertChapterMetadata(chapters: List<ChapterMetadataEntity>)

    @Query("SELECT chapter_id FROM chapter_metadata WHERE manga_id = :mangaId")
    protected abstract suspend fun getChapterMetadataIdsByManga(mangaId: String): List<String>

    @Query("DELETE FROM chapter_metadata WHERE chapter_id = :chapterId")
    protected abstract suspend fun deleteChapterMetadata(chapterId: String)

    @Transaction
    open suspend fun syncChapterMetadata(
        mangaId: String,
        chapters: List<ChapterMetadataEntity>,
        downloadedChapterIds: Set<String>,
    ) {
        val existingChapterIds = getChapterMetadataIdsByManga(mangaId)
        if (chapters.isNotEmpty()) {
            upsertChapterMetadata(chapters)
        }
        val retainedChapterIds = chapters.mapTo(mutableSetOf()) { it.chapterId } + downloadedChapterIds
        existingChapterIds
            .filterNot { it in retainedChapterIds }
            .forEach { deleteChapterMetadata(it) }
    }

    @Query("SELECT * FROM chapter_progress WHERE manga_id = :mangaId ORDER BY updated_at DESC")
    abstract fun getChapterProgressByManga(mangaId: String): Flow<List<ChapterProgressEntity>>

    @Query("SELECT * FROM chapter_progress")
    abstract suspend fun getAllProgress(): List<ChapterProgressEntity>

    @Query("SELECT * FROM chapter_progress WHERE chapter_id = :chapterId LIMIT 1")
    abstract suspend fun getChapterProgressByChapter(chapterId: String): ChapterProgressEntity?

    @Query("SELECT chapter_id FROM chapter_progress WHERE is_downloaded = 1")
    abstract suspend fun getDownloadedChapterIds(): List<String>

    @Query("UPDATE chapter_progress SET is_downloaded = 0 WHERE is_downloaded = 1")
    abstract suspend fun clearDownloadedChapterFlags()

    @Query("UPDATE chapter_progress SET is_downloaded = 0 WHERE chapter_id = :chapterId")
    abstract suspend fun clearDownloadedChapterFlag(chapterId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertChapterProgress(progress: ChapterProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertChapterProgressIfAbsent(progress: ChapterProgressEntity)

    @Query(
        """
        UPDATE chapter_progress
        SET status = :status,
            last_read_page = :lastReadPage,
            total_pages = :totalPages,
            updated_at = :updatedAt
        WHERE chapter_id = :chapterId
        """,
    )
    protected abstract suspend fun updateReadingProgress(
        chapterId: String,
        status: com.example.mybookslibrary.data.local.ChapterStatus,
        lastReadPage: Int,
        totalPages: Int,
        updatedAt: Long,
    )

    /**
     * Persists reading progress without overwriting download metadata.
     */
    @Transaction
    open suspend fun upsertReadingProgress(progress: ChapterProgressEntity) {
        insertChapterProgressIfAbsent(progress)
        updateReadingProgress(
            chapterId = progress.chapter_id,
            status = progress.status,
            lastReadPage = progress.last_read_page,
            totalPages = progress.total_pages,
            updatedAt = progress.updated_at,
        )
    }

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
