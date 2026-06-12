package com.example.mybookslibrary.ui.viewmodel

import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.DownloadedChapterDirInfo
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.local.dao.ChapterDao
import com.example.mybookslibrary.data.local.dao.DownloadedChapterInfo
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<ChapterDao>()
    private val cache = mockk<DownloadedChapterCache>()
    private val storage = mockk<OfflineDownloadStorage>()
    private val downloadManager = mockk<OfflineDownloadManager>()

    private fun build(): DownloadsViewModel =
        DownloadsViewModel(dao, cache, storage, downloadManager, mainDispatcherRule.dispatcher)

    @Test
    fun uiState_gopFilesystemVoiMetadata_orphanXuongCuoi() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { cache.downloadedChapterIds } returns MutableStateFlow(setOf("c1", "c2"))
            coEvery { storage.listDownloadedChapters() } returns
                listOf(
                    // orphan (mất metadata) đứng TRƯỚC trong filesystem — phải bị đẩy xuống cuối
                    DownloadedChapterDirInfo(mangaId = "m2", chapterId = "c2", sizeBytes = 5L),
                    DownloadedChapterDirInfo(mangaId = "m1", chapterId = "c1", sizeBytes = 10L),
                )
            coEvery { dao.getDownloadedChapterInfo(setOf("c2", "c1")) } returns
                listOf(
                    DownloadedChapterInfo(
                        mangaTitle = "One Piece",
                        volume = "1",
                        chapterNumber = "2",
                        chapterId = "c1",
                        mangaId = "m1",
                    ),
                )

            val vm = build()
            val state = vm.uiState.first { it.chapters != null }

            val chapters = checkNotNull(state.chapters)
            assertEquals(listOf("c1", "c2"), chapters.map { it.chapterId })
            assertEquals("One Piece", chapters[0].mangaTitle)
            assertEquals("Vol. 1 Ch. 2", chapters[0].chapterLabel)
            // Orphan: không title, không label — UI hiện fallback string
            assertNull(chapters[1].mangaTitle)
            assertNull(chapters[1].chapterLabel)
            assertEquals(15L, state.totalSizeBytes)
        }

    @Test
    fun uiState_banDauLaLoading_chaptersNull() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { cache.downloadedChapterIds } returns MutableStateFlow(emptySet())
            coEvery { storage.listDownloadedChapters() } returns emptyList()
            coEvery { dao.getDownloadedChapterInfo(emptySet()) } returns emptyList()

            val vm = build()

            // Đọc .value TRƯỚC khi advance dispatcher (StandardTestDispatcher queue task,
            // chưa chạy mapLatest) — đây chính là trạng thái loading mà UI thấy đầu tiên.
            assertNull(vm.uiState.value.chapters)
        }

    @Test
    fun deleteDownload_delegatesToDownloadManager() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { cache.downloadedChapterIds } returns MutableStateFlow(emptySet())
            coEvery { storage.listDownloadedChapters() } returns emptyList()
            coEvery { dao.getDownloadedChapterInfo(any()) } returns emptyList()
            coEvery { downloadManager.deleteDownload(any(), any()) } just Runs

            val vm = build()
            vm.deleteDownload("m1", "c1")
            advanceUntilIdle()

            coVerify(exactly = 1) { downloadManager.deleteDownload("m1", "c1") }
        }

    @Test
    fun deleteDownload_loiThiEmitDeleteFailed() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            every { cache.downloadedChapterIds } returns MutableStateFlow(emptySet())
            coEvery { storage.listDownloadedChapters() } returns emptyList()
            coEvery { dao.getDownloadedChapterInfo(any()) } returns emptyList()
            coEvery { downloadManager.deleteDownload(any(), any()) } throws RuntimeException("disk error")

            val vm = build()
            val events = mutableListOf<DownloadsEvent>()
            val job = launch { vm.events.collect { events.add(it) } }

            vm.deleteDownload("m1", "c1")
            advanceUntilIdle()
            job.cancel()

            assertEquals(listOf(DownloadsEvent.DELETE_FAILED), events)
        }
}
