package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import coil3.ImageLoader
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.AdjacentChapter
import com.example.mybookslibrary.domain.model.ReaderBackground
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.LoadReaderPagesUseCase
import com.example.mybookslibrary.domain.usecase.SyncReadingProgressUseCase
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

    private val userPreferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

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
    fun missingStartPageIndex_defaultsToZero() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = null)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.lastReadPageIndex)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withToggleOverlay_togglesOverlayVisibility() = runTest(mainDispatcherRule.dispatcher.scheduler) {
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
                    height = 1000f,
                ),
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
                    height = 1000f,
                ),
            )

            assertEquals(1, viewModel.state.value.lastReadPageIndex)
            assertEquals(1, navigationEvent.await().pageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withLeftTapInRtl_updatesCurrentPageAndEmitsNavigationEffect() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val viewModel = createViewModel(startPageIndex = 2)
            advanceUntilIdle()
            viewModel.onEvent(ReaderEvent.ChangeReadingMode(ReadingMode.RTL))
            val navigationEvent = async { viewModel.effects.first() as ReaderUiEffect.NavigateToPage }
            runCurrent()

            viewModel.onEvent(
                ReaderEvent.TapOnScreen(
                    x = 100f,
                    y = 500f,
                    width = 1000f,
                    height = 1000f,
                ),
            )

            assertEquals(3, viewModel.state.value.lastReadPageIndex)
            assertEquals(3, navigationEvent.await().pageIndex)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withTapInVerticalMode_togglesOverlay() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 2)
        advanceUntilIdle()
        viewModel.onEvent(ReaderEvent.ChangeReadingMode(ReadingMode.VERTICAL))

        viewModel.onEvent(
            ReaderEvent.TapOnScreen(
                x = 900f,
                y = 500f,
                width = 1000f,
                height = 1000f,
            ),
        )

        assertEquals(true, viewModel.state.value.isOverlayVisible)
        assertEquals(2, viewModel.state.value.lastReadPageIndex)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun storedReadingMode_isLoadedIntoInitialState() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0, storedReadingMode = ReadingMode.VERTICAL)
        advanceUntilIdle()

        assertEquals(ReadingMode.VERTICAL, viewModel.state.value.currentReadingMode)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun changeReadingMode_isSavedToDataStore() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        viewModel.onEvent(ReaderEvent.ChangeReadingMode(ReadingMode.RTL))
        advanceUntilIdle()

        coVerify { userPreferencesDataStore.setReaderReadingMode(ReadingMode.RTL) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_atPageBoundary_doesNotMovePastAvailablePages() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val firstPageViewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        firstPageViewModel.onEvent(
            ReaderEvent.TapOnScreen(
                x = 100f,
                y = 500f,
                width = 1000f,
                height = 1000f,
            ),
        )

        assertEquals(0, firstPageViewModel.state.value.lastReadPageIndex)

        val lastPageViewModel = createViewModel(startPageIndex = 7)
        advanceUntilIdle()

        lastPageViewModel.onEvent(
            ReaderEvent.TapOnScreen(
                x = 900f,
                y = 500f,
                width = 1000f,
                height = 1000f,
            ),
        )

        assertEquals(7, lastPageViewModel.state.value.lastReadPageIndex)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun onEvent_withPageActionSelected_emitsQuickSaveEffect() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()
        val effect = async { viewModel.effects.first() as ReaderUiEffect.QuickSavePage }
        runCurrent()

        viewModel.onEvent(
            ReaderEvent.PageLongPressed(
                pageUrl = "https://example.com/page-1.jpg",
                pageIndex = 0,
            ),
        )
        viewModel.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.QuickSave))

        assertEquals("https://example.com/page-1.jpg", effect.await().target.pageUrl)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun setBrightness_coercedToRangeAndPersisted() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        // SetBrightness chỉ update state (preview), KHÔNG ghi DataStore.
        viewModel.onEvent(ReaderEvent.SetBrightness(2.0f))
        advanceUntilIdle()
        assertEquals(1.0f, viewModel.state.value.brightness)

        viewModel.onEvent(ReaderEvent.SetBrightness(0.05f))
        advanceUntilIdle()
        assertEquals(0.15f, viewModel.state.value.brightness)
        coVerify(exactly = 0) { userPreferencesDataStore.setReaderBrightness(any()) }

        // CommitBrightness mới ghi DataStore (gọi khi thả slider).
        viewModel.onEvent(ReaderEvent.CommitBrightness)
        advanceUntilIdle()
        coVerify { userPreferencesDataStore.setReaderBrightness(0.15f) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun setBackground_updatesStateAndPersists() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        viewModel.onEvent(ReaderEvent.SetBackground(ReaderBackground.WHITE))
        advanceUntilIdle()

        assertEquals(ReaderBackground.WHITE, viewModel.state.value.background)
        coVerify { userPreferencesDataStore.setReaderBackground("WHITE") }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun comfortPreferences_loadedIntoState() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        every { userPreferencesDataStore.observeReaderKeepScreenOn() } returns flowOf(true)
        every { userPreferencesDataStore.observeReaderVolumeKeyNav() } returns flowOf(true)
        every { userPreferencesDataStore.observeReaderBrightness() } returns flowOf(0.5f)
        every { userPreferencesDataStore.observeReaderBackground() } returns flowOf("GRAY")
        every { userPreferencesDataStore.observeReaderAutoAdvance() } returns flowOf(true)
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.keepScreenOn)
        assertEquals(true, viewModel.state.value.volumeKeyNav)
        assertEquals(0.5f, viewModel.state.value.brightness)
        assertEquals(ReaderBackground.GRAY, viewModel.state.value.background)
        assertEquals(true, viewModel.state.value.autoAdvance)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun reachedTransitionPage_withAutoAdvanceAndNext_navigatesToNextChapter() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoAdvance = true)
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()
            val nav = async { viewModel.effects.first { it is ReaderUiEffect.NavigateToChapter } }
            runCurrent()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            val effect = nav.await() as ReaderUiEffect.NavigateToChapter
            assertEquals(NEXT_CHAPTER_ID, effect.chapterId)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun leftTransitionPage_cancelsPendingAutoAdvance() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoAdvance = true)
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()
            var navigated = false
            val job = backgroundScope.launch {
                viewModel.effects.collect { if (it is ReaderUiEffect.NavigateToChapter) navigated = true }
            }
            runCurrent()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            viewModel.onEvent(ReaderEvent.LeftTransitionPage) // lướt ngược lại trước khi hết delay
            advanceUntilIdle()

            assertEquals(false, navigated)
            job.cancel()
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun reachedTransitionPage_withoutAutoAdvance_doesNotNavigate() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoAdvance = false)
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()
            var navigated = false
            val job = backgroundScope.launch {
                viewModel.effects.collect { if (it is ReaderUiEffect.NavigateToChapter) navigated = true }
            }
            runCurrent()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            assertEquals(false, navigated)
            job.cancel()
        }

    private fun stubReaderPrefs(autoAdvance: Boolean = false) {
        every { userPreferencesDataStore.observeReaderKeepScreenOn() } returns flowOf(false)
        every { userPreferencesDataStore.observeReaderVolumeKeyNav() } returns flowOf(false)
        every { userPreferencesDataStore.observeReaderBrightness() } returns flowOf(1.0f)
        every { userPreferencesDataStore.observeReaderBackground() } returns flowOf("BLACK")
        every { userPreferencesDataStore.observeReaderAutoAdvance() } returns flowOf(autoAdvance)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun volumeKeyNextAndPrevPage_movesPage() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 2)
        advanceUntilIdle()

        viewModel.onEvent(ReaderEvent.VolumeKeyNextPage)
        assertEquals(3, viewModel.state.value.lastReadPageIndex)

        viewModel.onEvent(ReaderEvent.VolumeKeyPrevPage)
        assertEquals(2, viewModel.state.value.lastReadPageIndex)
    }

    private fun createViewModel(
        startPageIndex: Int?,
        storedReadingMode: ReadingMode = ReadingMode.LTR,
        nextChapter: AdjacentChapter? = null,
    ): ReaderViewModel {
        val loadReaderPagesUseCase = mockk<LoadReaderPagesUseCase>()
        val syncReadingProgressUseCase = mockk<SyncReadingProgressUseCase>(relaxed = true)
        val imageLoader = mockk<ImageLoader>(relaxed = true)
        coEvery { userPreferencesDataStore.getReaderReadingMode() } returns storedReadingMode
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns
            Result.success(
                listOf("page-1", "page-2", "page-3", "page-4", "page-5", "page-6", "page-7", "page-8"),
            )
        // Preload chương kế (Phase 4 PR-2a) gọi use case với chapterId khác → trả vài trang.
        coEvery { loadReaderPagesUseCase(MANGA_ID, NEXT_CHAPTER_ID) } returns
            Result.success(listOf("next-1", "next-2", "next-3"))

        val args =
            mutableMapOf<String, Any?>(
                "mangaId" to MANGA_ID,
                "chapterId" to CHAPTER_ID,
                "chapterTitle" to "Chapter 1",
                "startPageIndex" to (startPageIndex ?: 0),
            )

        val chapterDao = mockk<com.example.mybookslibrary.data.local.dao.ChapterDao>(relaxed = true)
        coEvery { chapterDao.getPrevChapter(any(), any()) } returns null
        coEvery { chapterDao.getNextChapter(any(), any()) } returns nextChapter

        return ReaderViewModel(
            application = RuntimeEnvironment.getApplication(),
            savedStateHandle = SavedStateHandle(args),
            loadReaderPagesUseCase = loadReaderPagesUseCase,
            syncReadingProgressUseCase = syncReadingProgressUseCase,
            chapterDao = chapterDao,
            tapZoneEvaluator = TapZoneEvaluator(),
            pageFileBuilder = ReaderPageFileBuilder(),
            userPreferencesDataStore = userPreferencesDataStore,
            imageLoader = imageLoader,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
        const val NEXT_CHAPTER_ID = "chapter-2"
        val NEXT_CHAPTER = AdjacentChapter(chapterId = NEXT_CHAPTER_ID, chapterNumber = "2", volume = null)
    }
}
