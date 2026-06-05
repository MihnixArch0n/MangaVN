package com.example.mybookslibrary.domain.usecase

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.MangaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LoadReaderPagesUseCaseTest {
    private val mangaRepository = mockk<MangaRepository>()
    private val downloadedChapterCache = mockk<DownloadedChapterCache>()
    private val offlineDownloadStorage = mockk<OfflineDownloadStorage>()

    private val useCase =
        LoadReaderPagesUseCase(
            mangaRepository = mangaRepository,
            downloadedChapterCache = downloadedChapterCache,
            offlineDownloadStorage = offlineDownloadStorage,
        )

    @Test
    fun downloadedChapter_withLocalPages_returnsFileUrisWithoutNetwork() =
        runTest {
            coEvery { downloadedChapterCache.isChapterDownloaded(CHAPTER_ID) } returns true
            coEvery { offlineDownloadStorage.getChapterPages(MANGA_ID, CHAPTER_ID) } returns
                listOf(File("/data/p0.jpg"), File("/data/p1.jpg"))

            val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

            assertEquals(2, pages.size)
            assertTrue(pages.first().startsWith("file:"))
            coVerify(exactly = 0) { mangaRepository.getChapterPages(CHAPTER_ID) }
        }

    @Test
    fun downloadedChapter_withoutLocalPages_fallsBackToNetwork() =
        runTest {
            coEvery { downloadedChapterCache.isChapterDownloaded(CHAPTER_ID) } returns true
            coEvery { offlineDownloadStorage.getChapterPages(MANGA_ID, CHAPTER_ID) } returns emptyList()
            coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.success(listOf("net-0", "net-1"))

            val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

            assertEquals(listOf("net-0", "net-1"), pages)
        }

    @Test
    fun chapterNotDownloaded_usesNetwork() =
        runTest {
            coEvery { downloadedChapterCache.isChapterDownloaded(CHAPTER_ID) } returns false
            coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.success(listOf("page-0"))

            val pages = useCase(MANGA_ID, CHAPTER_ID).getOrThrow()

            assertEquals(listOf("page-0"), pages)
            coVerify(exactly = 0) { offlineDownloadStorage.getChapterPages(any(), any()) }
        }

    @Test
    fun networkFailure_returnsFailure() =
        runTest {
            coEvery { downloadedChapterCache.isChapterDownloaded(CHAPTER_ID) } returns false
            coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.failure(IllegalStateException("boom"))

            val result = useCase(MANGA_ID, CHAPTER_ID)

            assertEquals("boom", result.exceptionOrNull()?.message)
        }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
