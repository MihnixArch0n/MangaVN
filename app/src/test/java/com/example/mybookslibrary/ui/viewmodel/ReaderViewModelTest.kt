package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
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
    fun onEvent_withToggleOverlay_togglesOverlayVisibility() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.ToggleOverlay)

            assertEquals(true, viewModel.state.value.isOverlayVisible)

            viewModel.onEvent(ReaderEvent.ToggleOverlay)

            assertEquals(false, viewModel.state.value.isOverlayVisible)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withRightTapInLtr_updatesCurrentPageAndEmitsNavigationEffect() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 2)
            advanceUntilIdle()
            val navigationEvent = async { viewModel.effects.first() as ReaderUiEffect.NavigateToPage }
            runCurrent()

            viewModel.onEvent(
                ReaderEvent.TapOnScreen(
                    x = 900f,
                    y = 500f,
                    width = 1000f,
                    height = 1000f
                )
            )

            assertEquals(3, viewModel.state.value.lastReadPageIndex)
            assertEquals(3, navigationEvent.await().pageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withLeftTapInLtr_updatesCurrentPageAndEmitsNavigationEffect() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 2)
            advanceUntilIdle()
            val navigationEvent = async { viewModel.effects.first() as ReaderUiEffect.NavigateToPage }
            runCurrent()

            viewModel.onEvent(
                ReaderEvent.TapOnScreen(
                    x = 100f,
                    y = 500f,
                    width = 1000f,
                    height = 1000f
                )
            )

            assertEquals(1, viewModel.state.value.lastReadPageIndex)
            assertEquals(1, navigationEvent.await().pageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_atPageBoundary_doesNotMovePastAvailablePages() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val firstPageViewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            firstPageViewModel.onEvent(
                ReaderEvent.TapOnScreen(
                    x = 100f,
                    y = 500f,
                    width = 1000f,
                    height = 1000f
                )
            )

            assertEquals(0, firstPageViewModel.state.value.lastReadPageIndex)

            val lastPageViewModel = createViewModel(startPageIndex = 7)
            advanceUntilIdle()

            lastPageViewModel.onEvent(
                ReaderEvent.TapOnScreen(
                    x = 900f,
                    y = 500f,
                    width = 1000f,
                    height = 1000f
                )
            )

            assertEquals(7, lastPageViewModel.state.value.lastReadPageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withPageActionSelected_emitsQuickSaveEffect() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()
            val effect = async { viewModel.effects.first() as ReaderUiEffect.QuickSavePage }
            runCurrent()

            viewModel.onEvent(ReaderEvent.PageLongPressed(pageUrl = "https://example.com/page-1.jpg", pageIndex = 0))
            viewModel.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.QuickSave))

            assertEquals("https://example.com/page-1.jpg", effect.await().target.pageUrl)
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
