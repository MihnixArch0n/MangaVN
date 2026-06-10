package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI

class LoadReaderPagesUseCaseTest {
    private val mangaRepository = mockk<MangaRepository>()
    private val downloadedChapterCache = mockk<DownloadedChapterCache>()
    private val offlineDownloadStorage = mockk<OfflineDownloadStorage>()
    private val offlineDownloadRepository = mockk<OfflineDownloadRepository>()

    private val useCase =
        LoadReaderPagesUseCase(
            mangaRepository = mangaRepository,
            downloadedChapterCache = downloadedChapterCache,
            offlineDownloadStorage = offlineDownloadStorage,
            offlineDownloadRepository = offlineDownloadRepository,
        )

    @Test
    fun downloadedChapter_withLocalPages_returnsFileUrisWithoutNetwork() = runTest {
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(setOf(CHAPTER_ID))
        coEvery { offlineDownloadStorage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID) } returns true
        coEvery { offlineDownloadStorage.getChapterPages(MANGA_ID, CHAPTER_ID) } returns
            listOf(File("/data/p0.jpg"), File("/data/p1.jpg"))

        val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

        assertEquals(2, pages.size)
        assertTrue(pages.first().startsWith("file:"))
        assertEquals("file", URI(pages.first()).scheme)
        coVerify(exactly = 0) { mangaRepository.getChapterPages(CHAPTER_ID) }
    }

    @Test
    fun ghostDownloadedChapter_repairsCacheAndFallsBackToNetwork() = runTest {
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(setOf(CHAPTER_ID))
        coEvery { offlineDownloadStorage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID) } returns false
        coEvery { offlineDownloadRepository.markChapterCorrupted(MANGA_ID, CHAPTER_ID) } just Runs
        coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.success(listOf("net-0", "net-1"))

        val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

        assertEquals(listOf("net-0", "net-1"), pages)
        coVerify(exactly = 0) { offlineDownloadStorage.getChapterPages(any(), any()) }
        coVerify(exactly = 1) { offlineDownloadRepository.markChapterCorrupted(MANGA_ID, CHAPTER_ID) }
    }

    @Test
    fun physicalDownloadMissingFromCache_repairsCacheAndReturnsLocalPages() = runTest {
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(emptySet())
        coEvery { offlineDownloadStorage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID) } returns true
        coEvery { offlineDownloadStorage.getChapterPages(MANGA_ID, CHAPTER_ID) } returns listOf(File("/data/p0.jpg"))
        every { downloadedChapterCache.addChapter(CHAPTER_ID) } just Runs

        val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

        assertEquals("file", URI(pages.single()).scheme)
        io.mockk.verify(exactly = 1) { downloadedChapterCache.addChapter(CHAPTER_ID) }
        coVerify(exactly = 0) { mangaRepository.getChapterPages(CHAPTER_ID) }
    }

    @Test
    fun chapterNotDownloaded_usesNetwork() = runTest {
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(emptySet())
        coEvery { offlineDownloadStorage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID) } returns false
        coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.success(listOf("page-0"))

        val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

        assertEquals(listOf("page-0"), pages)
        coVerify(exactly = 0) { offlineDownloadStorage.getChapterPages(any(), any()) }
    }

    @Test
    fun networkFailure_returnsFailure() = runTest {
        every { downloadedChapterCache.downloadedChapterIds } returns MutableStateFlow(emptySet())
        coEvery { offlineDownloadStorage.verifyDownloadedChapter(MANGA_ID, CHAPTER_ID) } returns false
        coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns
            Result.failure(IllegalStateException("boom"))

        val result = useCase(MANGA_ID, CHAPTER_ID)

        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
