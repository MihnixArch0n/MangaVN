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

    /** Đếm chapter đã đọc xong — cho Profile stats. */
    @Query("SELECT COUNT(*) FROM chapter_progress WHERE status = 'COMPLETED'")
    abstract fun observeCompletedChapterCount(): Flow<Int>

    /** Đếm chapter đang đọc dở — cho Statistics. */
    @Query("SELECT COUNT(*) FROM chapter_progress WHERE status = 'READING'")
    abstract fun observeReadingChapterCount(): Flow<Int>

    /** Tổng số chapter đã có progress — cho Statistics. */
    @Query("SELECT COUNT(*) FROM chapter_progress")
    abstract fun observeTotalProgressCount(): Flow<Int>

    /**
     * Chapter ĐÃ ĐỌC (READING/COMPLETED) trong 28 ngày gần nhất — cho Statistics chart.
     * Loại UNREAD: "đánh dấu chưa đọc" cũng ghi row mới, không được tính là hoạt động đọc.
     */
    @Query(
        """
        SELECT * FROM chapter_progress
        WHERE updated_at >= :cutoff AND status IN ('READING', 'COMPLETED')
        ORDER BY updated_at DESC
        """,
    )
    abstract fun observeRecentProgress(cutoff: Long): Flow<List<ChapterProgressEntity>>

    /** Top truyện có nhiều chương đã đọc nhất — cho Statistics RowChart. */
    @Query(
        """
        SELECT li.title AS title, COUNT(cp.chapter_id) AS chapterCount
        FROM chapter_progress cp
        INNER JOIN library_items li ON li.manga_id = cp.manga_id
        WHERE li.sync_status != 'PENDING_DELETE'
        GROUP BY cp.manga_id
        ORDER BY chapterCount DESC
        LIMIT 5
        """,
    )
    abstract fun observeTopReadManga(): Flow<List<TopMangaCount>>

    @Query("SELECT chapter_id FROM chapter_progress WHERE is_downloaded = 1")
    abstract suspend fun getDownloadedChapterIds(): List<String>

    /**
     * Thông tin hiển thị cho các chapter đã tải (màn Downloads). Nguồn sự thật về
     * "đã tải" là filesystem ([chapterIds] lấy từ DownloadedChapterCache) — query này
     * chỉ map id sang title/volume/chapter để hiển thị.
     *
     * LEFT JOIN (không INNER) vì manga đã rời library vẫn còn file chiếm dung lượng —
     * user phải thấy và xóa được; mangaTitle NULL = nhóm "Không còn trong thư viện".
     * Filter PENDING_DELETE ngay tại vế JOIN để manga chờ xóa cũng rơi về nhóm đó.
     */
    @Query(
        """
        SELECT li.title AS mangaTitle, cm.volume, cm.chapter_number AS chapterNumber,
               cm.chapter_id AS chapterId, cm.manga_id AS mangaId
        FROM chapter_metadata cm
        LEFT JOIN library_items li
          ON li.manga_id = cm.manga_id AND li.sync_status != 'PENDING_DELETE'
        WHERE cm.chapter_id IN (:chapterIds)
        ORDER BY li.title IS NULL, li.title, CAST(cm.chapter_number AS REAL)
        """,
    )
    abstract suspend fun getDownloadedChapterInfo(chapterIds: Set<String>): List<DownloadedChapterInfo>

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

    /**
     * Tổng số chương khả dụng của manga, gộp các bản dịch theo số chương
     * (oneshot không có chapter_number thì tính theo chapter_id) — cho auto-COMPLETED.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT COALESCE(chapter_number, chapter_id))
        FROM chapter_metadata
        WHERE manga_id = :mangaId AND is_unavailable = 0
        """,
    )
    abstract suspend fun countAvailableChapterNumbers(mangaId: String): Int

    /**
     * Số chương CHƯA đọc xong: một số chương được tính là đã đọc khi có ít nhất
     * một bản dịch COMPLETED — không bắt user đọc đủ mọi ngôn ngữ.
     */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT MAX(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) AS done
            FROM chapter_metadata m
            LEFT JOIN chapter_progress p ON p.chapter_id = m.chapter_id
            WHERE m.manga_id = :mangaId AND m.is_unavailable = 0
            GROUP BY COALESCE(m.chapter_number, m.chapter_id)
        ) WHERE done = 0
        """,
    )
    abstract suspend fun countUnfinishedChapterNumbers(mangaId: String): Int

    @Query(
        """
        SELECT chapter_id, chapter_number, volume FROM chapter_metadata
        WHERE manga_id = :mangaId
          AND feed_order < (SELECT feed_order FROM chapter_metadata WHERE chapter_id = :chapterId)
        ORDER BY feed_order DESC LIMIT 1
        """,
    )
    abstract suspend fun getPrevChapter(mangaId: String, chapterId: String): AdjacentChapter?

    @Query(
        """
        SELECT chapter_id, chapter_number, volume FROM chapter_metadata
        WHERE manga_id = :mangaId
          AND feed_order > (SELECT feed_order FROM chapter_metadata WHERE chapter_id = :chapterId)
        ORDER BY feed_order ASC LIMIT 1
        """,
    )
    abstract suspend fun getNextChapter(mangaId: String, chapterId: String): AdjacentChapter?

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

/** Kết quả projection cho [ChapterDao.observeTopReadManga]. */
data class TopMangaCount(
    val title: String,
    val chapterCount: Int,
)

data class AdjacentChapter(
    @androidx.room.ColumnInfo(name = "chapter_id") val chapterId: String,
    @androidx.room.ColumnInfo(name = "chapter_number") val chapterNumber: String?,
    @androidx.room.ColumnInfo(name = "volume") val volume: String?,
) {
    fun buildTitle(): String = buildString {
        volume?.takeIf { it.isNotBlank() }?.let { append("Vol. $it ") }
        chapterNumber?.takeIf { it.isNotBlank() }?.let { append("Ch. $it") }
        if (isEmpty()) append("Oneshot")
    }
}

/**
 * Projection cho [ChapterDao.getDownloadedChapterInfo] — màn Downloads.
 * [mangaTitle] NULL = manga không còn trong library (vẫn hiện để user xóa file).
 */
data class DownloadedChapterInfo(
    val mangaTitle: String?,
    val volume: String?,
    val chapterNumber: String?,
    val chapterId: String,
    val mangaId: String,
) {
    /** "Vol. X Ch. Y" — cùng format với [AdjacentChapter.buildTitle]. */
    fun buildChapterLabel(): String = buildString {
        volume?.takeIf { it.isNotBlank() }?.let { append("Vol. $it ") }
        chapterNumber?.takeIf { it.isNotBlank() }?.let { append("Ch. $it") }
        if (isEmpty()) append("Oneshot")
    }
}
