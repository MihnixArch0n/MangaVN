package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ReaderTapAction
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun startPageIndexFromSavedStateHandle_isEmittedInInitialState() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 7)
            advanceUntilIdle()

            assertEquals(7, viewModel.state.value.lastReadPageIndex)
            assertEquals("Chapter 1", viewModel.state.value.chapterTitle)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun missingStartPageIndex_defaultsToZero() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = null)
            advanceUntilIdle()

            assertEquals(0, viewModel.state.value.lastReadPageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun navigateToPage_withToggleOverlay_togglesOverlayVisibility() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            viewModel.navigateToPage(ReaderTapAction.TOGGLE_OVERLAY)

            assertEquals(true, viewModel.state.value.isOverlayVisible)

            viewModel.navigateToPage(ReaderTapAction.TOGGLE_OVERLAY)

            assertEquals(false, viewModel.state.value.isOverlayVisible)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun navigateToPage_withNextPage_updatesCurrentPageAndEmitsNavigationEvent() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 2)
            advanceUntilIdle()
            val navigationEvent = async { viewModel.pageNavigationEvent.first() }
            runCurrent()

            viewModel.navigateToPage(ReaderTapAction.NEXT_PAGE)

            assertEquals(3, viewModel.state.value.lastReadPageIndex)
            assertEquals(3, navigationEvent.await())
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun navigateToPage_withPreviousPage_updatesCurrentPageAndEmitsNavigationEvent() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 2)
            advanceUntilIdle()
            val navigationEvent = async { viewModel.pageNavigationEvent.first() }
            runCurrent()

            viewModel.navigateToPage(ReaderTapAction.PREVIOUS_PAGE)

            assertEquals(1, viewModel.state.value.lastReadPageIndex)
            assertEquals(1, navigationEvent.await())
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun navigateToPage_atPageBoundary_doesNotMovePastAvailablePages() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val firstPageViewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            firstPageViewModel.navigateToPage(ReaderTapAction.PREVIOUS_PAGE)

            assertEquals(0, firstPageViewModel.state.value.lastReadPageIndex)

            val lastPageViewModel = createViewModel(startPageIndex = 7)
            advanceUntilIdle()

            lastPageViewModel.navigateToPage(ReaderTapAction.NEXT_PAGE)

            assertEquals(7, lastPageViewModel.state.value.lastReadPageIndex)
        }

    private fun createViewModel(startPageIndex: Int?): ReaderViewModel {
        val mangaRepository = mockk<MangaRepository>()
        val libraryRepository = mockk<LibraryRepository>(relaxed = true)
        val downloadedChapterCache = mockk<DownloadedChapterCache>()
        val offlineDownloadStorage = mockk<OfflineDownloadStorage>()
        coEvery { mangaRepository.getChapterPages(CHAPTER_ID) } returns Result.success(
            listOf("page-1", "page-2", "page-3", "page-4", "page-5", "page-6", "page-7", "page-8")
        )
        every { downloadedChapterCache.isChapterDownloadedFlow(CHAPTER_ID) } returns flowOf(false)

        val args = mutableMapOf<String, Any?>(
            "mangaId" to MANGA_ID,
            "chapterId" to CHAPTER_ID,
            "chapterTitle" to "Chapter 1"
        )
        if (startPageIndex != null) {
            args["startPageIndex"] = startPageIndex
        }

        return ReaderViewModel(
            application = RuntimeEnvironment.getApplication(),
            savedStateHandle = SavedStateHandle(args),
            mangaRepository = mangaRepository,
            libraryRepository = libraryRepository,
            downloadedChapterCache = downloadedChapterCache,
            offlineDownloadStorage = offlineDownloadStorage,
            ioDispatcher = mainDispatcherRule.dispatcher
        )
    }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
