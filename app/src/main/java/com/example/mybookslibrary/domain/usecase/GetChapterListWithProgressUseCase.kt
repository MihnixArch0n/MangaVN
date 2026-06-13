package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.local.ChapterMetadataEntity
import com.example.mybookslibrary.data.local.ChapterProgressEntity
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.toMetadataEntity
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import com.example.mybookslibrary.domain.model.ChapterDownloadState
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChapterListResult(
    val chapters: List<ChapterWithProgressModel>,
    val availableLanguages: List<String>,
    val selectedLanguage: String,
)

class GetChapterListWithProgressUseCase
    @Inject
    constructor(
        private val mangaRepository: MangaRepository,
        private val chapterDao: ChapterDao,
        private val offlineDownloadRepository: OfflineDownloadRepository,
        private val downloadedChapterCache: DownloadedChapterCache,
        private val userPreferencesDataStore: UserPreferencesDataStore,
    ) {
        /**
         * Emits locally cached chapters immediately and silently refreshes them from MangaDex.
         *
         * Progress is keyed by chapter_id so each chapter keeps its own status and
         * last-read page independently when another chapter is opened.
         */
        operator fun invoke(mangaId: String): Flow<ChapterListResult> =
            flow {
                refreshDownloadedChapterCache()
                coroutineScope {
                    launch { refreshChapterMetadata(mangaId) }
                    emitAll(
                        combine(
                            chapterDao.getChaptersByMangaIdFlow(mangaId),
                            chapterDao.getChapterProgressByManga(mangaId),
                            offlineDownloadRepository.observeQueueByManga(mangaId),
                            downloadedChapterCache.downloadedChapterIds,
                            userPreferencesDataStore.observePreferredChapterLanguage(),
                        ) { metadata, progressList, queueList, downloadedIds, prefLang ->
                            val rawChapters = mapChapterSnapshot(
                                mangaId = mangaId,
                                metadata = metadata,
                                progressList = progressList,
                                queueList = queueList,
                                downloadedIds = downloadedIds,
                            )

                            val availableLanguages = rawChapters.mapNotNull {
                                it.translatedLanguage
                            }.distinct().sorted()

                            val selectedLanguage = if (prefLang == "") {
                                "" // All
                            } else if (availableLanguages.contains(prefLang)) {
                                prefLang
                            } else if (availableLanguages.contains("en")) {
                                "en"
                            } else {
                                availableLanguages.firstOrNull() ?: ""
                            }

                            val filteredChapters = if (selectedLanguage == "") {
                                rawChapters
                            } else {
                                rawChapters.filter { it.translatedLanguage == selectedLanguage }
                            }

                            ChapterListResult(
                                chapters = filteredChapters,
                                availableLanguages = availableLanguages,
                                selectedLanguage = selectedLanguage,
                            )
                        },
                    )
                }
            }

        @Suppress("TooGenericExceptionCaught")
        private suspend fun refreshDownloadedChapterCache() {
            try {
                downloadedChapterCache.scanFilesystem()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (t: Throwable) {
                Timber.w(t, "Chapter filesystem rescan failed")
            }
        }

        private suspend fun refreshChapterMetadata(mangaId: String) {
            val result = mangaRepository.getMangaFeed(mangaId)
            val error = result.exceptionOrNull()
            if (error != null) {
                if (error is CancellationException) throw error
                Timber.w(error, "Chapter feed refresh failed silently: mangaId=%s", mangaId)
                return
            }

            val now = System.currentTimeMillis()
            val chapters = result.getOrThrow()
            chapterDao.syncChapterMetadata(
                mangaId = mangaId,
                chapters = chapters.mapIndexed { index, chapter -> chapter.toMetadataEntity(index, now) },
                downloadedChapterIds = downloadedChapterCache.downloadedChapterIds.value,
            )
            Timber.d("Chapter feed refresh complete: mangaId=%s chapters=%d", mangaId, chapters.size)
        }
    }

private fun mapChapterSnapshot(
    mangaId: String,
    metadata: List<ChapterMetadataEntity>,
    progressList: List<ChapterProgressEntity>,
    queueList: List<DownloadQueueEntity>,
    downloadedIds: Set<String>,
): List<ChapterWithProgressModel> {
    val progressMap = progressList.associateBy { it.chapter_id }
    val queueMap = queueList.associateBy { it.chapter_id }
    val cachedIds = metadata.mapTo(mutableSetOf()) { it.chapterId }

    val cachedModels =
        metadata
            .filterNot { it.isUnavailable }
            .map { chapter ->
                chapter.toChapterWithProgress(
                    progress = progressMap[chapter.chapterId],
                    queue = queueMap[chapter.chapterId],
                    isDownloaded = chapter.chapterId in downloadedIds,
                )
            }
    val downloadedFallbacks =
        progressList
            .asSequence()
            .filter { it.chapter_id in downloadedIds && it.chapter_id !in cachedIds }
            .map { progress ->
                ChapterWithProgressModel(
                    chapterId = progress.chapter_id,
                    mangaId = mangaId,
                    volume = null,
                    chapterNumber = null,
                    title = null,
                    status = progress.status.toDomainStatus(),
                    lastReadPage = progress.last_read_page,
                    totalPages = progress.total_pages.coerceAtLeast(0),
                    downloadState = queueMap[progress.chapter_id].toDownloadState(isDownloaded = true),
                )
            }.toList()

    Timber.v(
        "Chapter snapshot: mangaId=%s metadata=%d progress=%d queue=%d downloaded=%d fallbacks=%d",
        mangaId,
        metadata.size,
        progressList.size,
        queueList.size,
        downloadedIds.size,
        downloadedFallbacks.size,
    )
    return cachedModels + downloadedFallbacks
}

private fun ChapterMetadataEntity.toChapterWithProgress(
    progress: ChapterProgressEntity?,
    queue: DownloadQueueEntity?,
    isDownloaded: Boolean,
): ChapterWithProgressModel {
    val totalPages =
        when {
            progress != null && progress.total_pages > 0 -> progress.total_pages
            pages > 0 -> pages
            else -> 0
        }
    return ChapterWithProgressModel(
        chapterId = chapterId,
        mangaId = mangaId,
        volume = volume,
        chapterNumber = chapterNumber,
        title = title,
        status = progress?.status.toDomainStatus(),
        lastReadPage = progress?.last_read_page ?: 0,
        totalPages = totalPages,
        translatedLanguage = translatedLanguage,
        downloadState = queue.toDownloadState(isDownloaded),
    )
}

private fun ChapterStatus?.toDomainStatus(): ChapterReadingStatus =
    when (this) {
        ChapterStatus.READING -> ChapterReadingStatus.READING
        ChapterStatus.COMPLETED -> ChapterReadingStatus.COMPLETED
        null,
        ChapterStatus.UNREAD,
        -> ChapterReadingStatus.UNREAD
    }

private fun DownloadQueueEntity?.toDownloadState(isDownloaded: Boolean): ChapterDownloadState {
    if (isDownloaded) {
        return ChapterDownloadState(status = ChapterDownloadStatus.DOWNLOADED, progressPercent = 100)
    }

    if (this == null) {
        return ChapterDownloadState()
    }

    return when (status) {
        DownloadStatus.PENDING ->
            ChapterDownloadState(
                status = ChapterDownloadStatus.PENDING,
                progressPercent = progress_percent,
            )
        DownloadStatus.DOWNLOADING ->
            ChapterDownloadState(
                status = ChapterDownloadStatus.DOWNLOADING,
                progressPercent = progress_percent,
            )
        DownloadStatus.COMPLETED -> ChapterDownloadState()
        DownloadStatus.ERROR ->
            ChapterDownloadState(
                status = ChapterDownloadStatus.ERROR,
                progressPercent = progress_percent,
                errorMessage = error_msg,
            )
    }
}
