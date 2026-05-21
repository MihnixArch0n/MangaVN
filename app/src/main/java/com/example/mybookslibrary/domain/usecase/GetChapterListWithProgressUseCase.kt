package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.local.ChapterStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import timber.log.Timber

class GetChapterListWithProgressUseCase @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterDao: ChapterDao
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
            chapterDao.getChapterProgressByManga(mangaId).map { progressList ->
                Timber.d(
                    "GetChapterListWithProgressUseCase progress snapshot: mangaId=%s progressRows=%d",
                    mangaId,
                    progressList.size
                )
                val progressMap = progressList.associateBy { it.chapter_id }

                remoteChapters.map { chapter ->
                    val progress = progressMap[chapter.id]
                    val totalPages = when {
                        progress != null && progress.total_pages > 0 -> progress.total_pages
                        chapter.pages > 0 -> chapter.pages
                        else -> 0
                    }
                    val mappedStatus = progress?.status.toDomainStatus()
                    Timber.d(
                        "GetChapterListWithProgressUseCase mapped: chapterId=%s status=%s lastReadPage=%d totalPages=%d progressFound=%s",
                        chapter.id,
                        mappedStatus,
                        progress?.last_read_page ?: 0,
                        totalPages,
                        progress != null
                    )

                    ChapterWithProgressModel(
                        chapterId = chapter.id,
                        mangaId = chapter.mangaId,
                        volume = chapter.volume,
                        chapterNumber = chapter.chapterNumber,
                        title = chapter.title,
                        status = mappedStatus,
                        lastReadPage = progress?.last_read_page ?: 0,
                        totalPages = totalPages
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
