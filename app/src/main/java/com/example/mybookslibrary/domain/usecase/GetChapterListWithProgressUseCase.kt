package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ChapterDownloadState
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import timber.log.Timber

class GetChapterListWithProgressUseCase @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterDao: ChapterDao,
    private val offlineDownloadRepository: OfflineDownloadRepository,
    private val downloadedChapterCache: DownloadedChapterCache
) {
    /**
     * Combines the remote chapter feed with locally persisted progress.
     *
     * Progress is keyed by chapter_id so each chapter keeps its own status and
     * last-read page independently when another chapter is opened.
     */
    operator fun invoke(mangaId: String): Flow<List<ChapterWithProgressModel>> = flow {
        val remoteChapters = mangaRepository.getMangaFeed(mangaId).getOrThrow()
        Timber.d(
            "GetChapterListWithProgressUseCase start: mangaId=%s remoteChapters=%d",
            mangaId,
            remoteChapters.size
        )

        emitAll(
            combine(
                chapterDao.getChapterProgressByManga(mangaId),
                offlineDownloadRepository.observeQueueByManga(mangaId),
                downloadedChapterCache.downloadedChapterIds
            ) { progressList, queueList, downloadedIds ->
                Timber.v(
                    "GetChapterListWithProgressUseCase snapshot: mangaId=%s progressRows=%d queueRows=%d downloaded=%d",
                    mangaId,
                    progressList.size,
                    queueList.size,
                    downloadedIds.size
                )
                val progressMap = progressList.associateBy { it.chapter_id }
                val queueMap = queueList.associateBy { it.chapter_id }

                remoteChapters.map { chapter ->
                    val progress = progressMap[chapter.id]
                    val downloadState = queueMap[chapter.id].toDownloadState(chapter.id in downloadedIds)
                    val totalPages = when {
                        progress != null && progress.total_pages > 0 -> progress.total_pages
                        chapter.pages > 0 -> chapter.pages
                        else -> 0
                    }
                    val mappedStatus = progress?.status.toDomainStatus()

                    ChapterWithProgressModel(
                        chapterId = chapter.id,
                        mangaId = chapter.mangaId,
                        volume = chapter.volume,
                        chapterNumber = chapter.chapterNumber,
                        title = chapter.title,
                        status = mappedStatus,
                        lastReadPage = progress?.last_read_page ?: 0,
                        totalPages = totalPages,
                        downloadState = downloadState
                    )
                }
            }
        )
    }
}

private fun ChapterStatus?.toDomainStatus(): ChapterReadingStatus = when (this) {
    ChapterStatus.READING -> ChapterReadingStatus.READING
    ChapterStatus.COMPLETED -> ChapterReadingStatus.COMPLETED
    null,
    ChapterStatus.UNREAD -> ChapterReadingStatus.UNREAD
}

private fun DownloadQueueEntity?.toDownloadState(isDownloaded: Boolean): ChapterDownloadState {
    if (isDownloaded) {
        return ChapterDownloadState(status = ChapterDownloadStatus.DOWNLOADED, progressPercent = 100)
    }

    if (this == null) {
        return ChapterDownloadState()
    }

    return when (status) {
        DownloadStatus.PENDING -> ChapterDownloadState(
            status = ChapterDownloadStatus.PENDING,
            progressPercent = progress_percent
        )
        DownloadStatus.DOWNLOADING -> ChapterDownloadState(
            status = ChapterDownloadStatus.DOWNLOADING,
            progressPercent = progress_percent
        )
        DownloadStatus.COMPLETED -> ChapterDownloadState()
        DownloadStatus.ERROR -> ChapterDownloadState(
            status = ChapterDownloadStatus.ERROR,
            progressPercent = progress_percent,
            errorMessage = error_msg
        )
    }
}
