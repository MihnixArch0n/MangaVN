package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.local.DownloadQueueEntity
import com.example.mybookslibrary.data.local.DownloadStatus
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import com.example.mybookslibrary.domain.model.ChapterDownloadStatus
import com.example.mybookslibrary.domain.model.ChapterModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phủ các nhánh download-state còn lại của use case: PENDING / DOWNLOADING / ERROR
 * và trường hợp chapter không trang, không progress.
 */
class GetChapterListWithProgressUseCaseCoverageTest {
    private val mangaRepository = mockk<MangaRepository>()
    private val chapterDao = mockk<ChapterDao>()
    private val offlineDownloadRepository = mockk<OfflineDownloadRepository>()
    private val downloadedChapterCache = mockk<DownloadedChapterCache>()
    private val useCase =
        GetChapterListWithProgressUseCase(
            mangaRepository,
            chapterDao,
            offlineDownloadRepository,
            downloadedChapterCache,
        )

    private fun stubFeed(pages: Int = 12) {
        coEvery { mangaRepository.getMangaFeed(MANGA_ID) } returns
            Result.success(
                listOf(
                    ChapterModel(
                        id = CHAPTER_ID,
                        mangaId = MANGA_ID,
                        volume = null,
                        chapterNumber = "1",
                        title = null,
                        pages = pages,
                        isUnavailable = false,
                    ),
                ),
            )
        every { chapterDao.getChapterProgressByManga(MANGA_ID) } returns flowOf(emptyList())
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(emptySet())
    }

    private fun stubQueue(
        status: DownloadStatus,
        progress: Int,
        error: String? = null,
    ) {
        every { offlineDownloadRepository.observeQueueByManga(MANGA_ID) } returns
            flowOf(
                listOf(
                    DownloadQueueEntity(
                        chapter_id = CHAPTER_ID,
                        manga_id = MANGA_ID,
                        status = status,
                        progress_percent = progress,
                        error_msg = error,
                    ),
                ),
            )
    }

    @Test
    fun pendingQueue_mapsToPendingState() =
        runTest {
            stubFeed()
            stubQueue(DownloadStatus.PENDING, progress = 30)

            val state = useCase(MANGA_ID).first().single().downloadState
            assertEquals(ChapterDownloadStatus.PENDING, state.status)
            assertEquals(30, state.progressPercent)
        }

    @Test
    fun downloadingQueue_mapsToDownloadingState() =
        runTest {
            stubFeed()
            stubQueue(DownloadStatus.DOWNLOADING, progress = 55)

            val state = useCase(MANGA_ID).first().single().downloadState
            assertEquals(ChapterDownloadStatus.DOWNLOADING, state.status)
            assertEquals(55, state.progressPercent)
        }

    @Test
    fun errorQueue_mapsToErrorStateWithMessage() =
        runTest {
            stubFeed()
            stubQueue(DownloadStatus.ERROR, progress = 40, error = "disk full")

            val state = useCase(MANGA_ID).first().single().downloadState
            assertEquals(ChapterDownloadStatus.ERROR, state.status)
            assertEquals("disk full", state.errorMessage)
        }

    @Test
    fun chapterWithoutPagesOrProgress_totalPagesIsZero() =
        runTest {
            stubFeed(pages = 0)
            every { offlineDownloadRepository.observeQueueByManga(MANGA_ID) } returns flowOf(emptyList())

            assertEquals(0, useCase(MANGA_ID).first().single().totalPages)
        }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
