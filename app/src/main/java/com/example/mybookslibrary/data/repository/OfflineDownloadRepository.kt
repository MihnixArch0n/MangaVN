package com.example.mybookslibrary.data.repository

import androidx.room.withTransaction
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.local.AppDatabase
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadQueueDao
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates download queue persistence, download settings, and downloaded chapter state.
 */
@Singleton
class OfflineDownloadRepository
    @Inject
    constructor(
        private val downloadQueueDao: DownloadQueueDao,
        private val chapterDao: ChapterDao,
        private val database: AppDatabase,
        private val preferencesDataStore: UserPreferencesDataStore,
        private val downloadedChapterCache: DownloadedChapterCache,
    ) {
        fun observeQueue(): Flow<List<DownloadQueueEntity>> = downloadQueueDao.observeQueue()

        fun observeQueueByManga(mangaId: String): Flow<List<DownloadQueueEntity>> = downloadQueueDao.observeQueueByManga(mangaId)

        fun observeQueueByChapter(chapterId: String): Flow<DownloadQueueEntity?> = downloadQueueDao.observeQueueByChapter(chapterId)

        suspend fun getQueueByChapter(chapterId: String): DownloadQueueEntity? = downloadQueueDao.getQueueByChapter(chapterId)

        suspend fun enqueueChapter(
            mangaId: String,
            chapterId: String,
        ) {
            Timber.d("enqueueChapter: mangaId=%s chapterId=%s", mangaId, chapterId)
            downloadQueueDao.upsert(
                DownloadQueueEntity(
                    chapter_id = chapterId,
                    manga_id = mangaId,
                    status = DownloadStatus.PENDING,
                    progress_percent = 0,
                    error_msg = null,
                ),
            )
        }

        suspend fun updateQueueStatus(
            chapterId: String,
            status: DownloadStatus,
            progressPercent: Int,
            errorMessage: String? = null,
        ) {
            val boundedProgress = progressPercent.coerceIn(0, 100)
            Timber.d(
                "updateQueueStatus: chapterId=%s status=%s progress=%d error=%s",
                chapterId,
                status,
                boundedProgress,
                errorMessage,
            )
            downloadQueueDao.updateStatus(
                chapterId = chapterId,
                status = status,
                progressPercent = boundedProgress,
                errorMessage = errorMessage,
            )
        }

        suspend fun removeQueuedChapter(chapterId: String) {
            Timber.d("removeQueuedChapter: chapterId=%s", chapterId)
            downloadQueueDao.deleteByChapter(chapterId)
        }

        suspend fun removeQueuedManga(mangaId: String) {
            Timber.d("removeQueuedManga: mangaId=%s", mangaId)
            downloadQueueDao.deleteByManga(mangaId)
        }

        suspend fun markChapterDownloaded(
            mangaId: String,
            chapterId: String,
            totalPages: Int = 0,
        ) {
            val now = System.currentTimeMillis()
            Timber.d("markChapterDownloaded: mangaId=%s chapterId=%s totalPages=%d", mangaId, chapterId, totalPages)
            database.withTransaction {
                val current = chapterDao.getChapterProgressByChapter(chapterId)
                chapterDao.upsertReadingProgress(
                    current?.copy(
                        manga_id = mangaId,
                        total_pages = current.total_pages.takeIf { it > 0 } ?: totalPages.coerceAtLeast(0),
                        updated_at = now,
                    ) ?: ChapterProgressEntity(
                        chapter_id = chapterId,
                        manga_id = mangaId,
                        total_pages = totalPages.coerceAtLeast(0),
                        updated_at = now,
                    ),
                )
                downloadQueueDao.deleteByChapter(chapterId)
            }
            downloadedChapterCache.addChapter(chapterId)
        }

        suspend fun markChapterNotDownloaded(chapterId: String) {
            Timber.d("markChapterNotDownloaded: chapterId=%s", chapterId)
            database.withTransaction {
                chapterDao.clearDownloadedChapterFlag(chapterId)
                downloadQueueDao.deleteByChapter(chapterId)
            }
            downloadedChapterCache.removeChapter(chapterId)
        }

        suspend fun markChapterCorrupted(mangaId: String, chapterId: String) {
            Timber.d("markChapterCorrupted: mangaId=%s chapterId=%s", mangaId, chapterId)
            downloadQueueDao.upsert(
                DownloadQueueEntity(
                    chapter_id = chapterId,
                    manga_id = mangaId,
                    status = DownloadStatus.ERROR,
                    progress_percent = 0,
                    error_msg = "Thiếu trang (Missing pages)"
                )
            )
            downloadedChapterCache.removeChapter(chapterId)
        }

        fun observeDownloadOnlyOnWifi(): Flow<Boolean> = preferencesDataStore.observeDownloadOnlyOnWifi()

        suspend fun getDownloadOnlyOnWifi(): Boolean = preferencesDataStore.getDownloadOnlyOnWifi()

        suspend fun setDownloadOnlyOnWifi(enabled: Boolean) {
            Timber.d("setDownloadOnlyOnWifi: enabled=%s", enabled)
            preferencesDataStore.setDownloadOnlyOnWifi(enabled)
        }
    }
