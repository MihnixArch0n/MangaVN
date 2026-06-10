package com.example.mybookslibrary.data.download

import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadQueueDao
import com.example.mybookslibrary.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory index of chapters that have completed offline downloads.
 *
 * The filesystem is the source of truth. Room download flags are read once only
 * to migrate downloads created before completion markers existed.
 */
@Singleton
class DownloadedChapterCache
    @Inject
    constructor(
        private val chapterDao: ChapterDao,
        private val downloadQueueDao: DownloadQueueDao,
        private val storage: OfflineDownloadStorage,
        @param:ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        private val _downloadedChapterIds = MutableStateFlow<Set<String>>(emptySet())
        private val initialization = CompletableDeferred<Unit>()

        val downloadedChapterIds: StateFlow<Set<String>> = _downloadedChapterIds.asStateFlow()

        init {
            applicationScope.launch {
                try {
                    val legacyDownloadedIds = chapterDao.getDownloadedChapterIds().toSet()
                    storage.backfillCompletionMarkers(legacyDownloadedIds)
                    chapterDao.clearDownloadedChapterFlags()
                    scanDownloadedChapters()
                    
                    val corruptedChapters = storage.scanCorruptedChapters()
                    for ((mangaId, chapterId) in corruptedChapters) {
                        downloadQueueDao.upsert(
                            DownloadQueueEntity(
                                chapter_id = chapterId,
                                manga_id = mangaId,
                                status = DownloadStatus.ERROR,
                                progress_percent = 0,
                                error_msg = "Thiếu trang (Missing pages)"
                            )
                        )
                    }
                    
                    Timber.d("DownloadedChapterCache initialized: count=%d, corrupted=%d", downloadedChapterIds.value.size, corruptedChapters.size)
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (t: Throwable) {
                    Timber.e(t, "DownloadedChapterCache initialization failed")
                } finally {
                    initialization.complete(Unit)
                }
            }
        }

        /**
         * Observes whether [chapterId] exists in the downloaded chapter set.
         */
        fun isChapterDownloadedFlow(chapterId: String): Flow<Boolean> = downloadedChapterIds.map { chapterId in it }

        /**
         * Waits for the startup disk scan before returning a one-shot lookup.
         */
        suspend fun isChapterDownloaded(chapterId: String): Boolean {
            initialization.await()
            return chapterId in downloadedChapterIds.value
        }

        /**
         * Refreshes the in-memory index from completed filesystem downloads.
         */
        suspend fun scanDownloadedChapters() {
            _downloadedChapterIds.value = storage.scanDownloadedChapters()
        }

        /**
         * Adds [chapterId] after its completion marker has been written.
         */
        fun addChapter(chapterId: String) {
            _downloadedChapterIds.update { it + chapterId }
            Timber.d("DownloadedChapterCache addChapter: chapterId=%s", chapterId)
        }

        /**
         * Removes [chapterId] from the downloaded set after deletion succeeds.
         */
        fun removeChapter(chapterId: String) {
            _downloadedChapterIds.update { it - chapterId }
            Timber.d("DownloadedChapterCache removeChapter: chapterId=%s", chapterId)
        }
    }
