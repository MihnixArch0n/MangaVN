package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import coil3.ImageLoader
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.data.local.dao.AdjacentChapter
import com.example.mybookslibrary.data.repository.OfflineDownloadRepository
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val offlineDownloadManager = mockk<OfflineDownloadManager>(relaxed = true)
    private val offlineDownloadRepository = mockk<OfflineDownloadRepository>(relaxed = true)
    private val downloadedChapterCache = mockk<DownloadedChapterCache>(relaxed = true)
    private val chapterDao = mockk<com.example.mybookslibrary.data.local.dao.ChapterDao>(relaxed = true)
    private val loadReaderPagesUseCase = mockk<LoadReaderPagesUseCase>()

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
    fun chapterPages_areNotLoadedBeforeStoredReadingModeIsResolved() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val modeResult = CompletableDeferred<ReadingMode>()
            val loadReaderPagesUseCase = mockk<LoadReaderPagesUseCase>()
            coEvery { userPreferencesDataStore.getReaderReadingMode() } coAnswers { modeResult.await() }

            val viewModel =
                createViewModel(
                    startPageIndex = 0,
                    loadReaderPagesUseCase = loadReaderPagesUseCase,
                    stubStoredReadingMode = false,
                    readerPages = listOf("page-1"),
                )
            runCurrent()

            coVerify(exactly = 0) { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) }

            modeResult.complete(ReadingMode.RTL)
            advanceUntilIdle()

            assertEquals(ReadingMode.RTL, viewModel.state.value.currentReadingMode)
            assertEquals(listOf("page-1"), viewModel.state.value.pages)
            coVerify(exactly = 1) { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) }
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
    fun perMangaOverride_isLoadedOverGlobalDefault() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0, storedReadingMode = ReadingMode.LTR)
        // Stub SAU createViewModel để thắng default any()->null; override RTL thắng global LTR.
        coEvery { userPreferencesDataStore.getReaderModeForManga(MANGA_ID) } returns ReadingMode.RTL
        advanceUntilIdle()

        assertEquals(ReadingMode.RTL, viewModel.state.value.currentReadingMode)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun changeReadingMode_persistsPerMangaOverride() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = createViewModel(startPageIndex = 0)
        advanceUntilIdle()

        viewModel.onEvent(ReaderEvent.ChangeReadingMode(ReadingMode.VERTICAL))
        advanceUntilIdle()

        // Lưu override per-manga + global default.
        coVerify { userPreferencesDataStore.setReaderModeForManga(MANGA_ID, ReadingMode.VERTICAL) }
        coVerify { userPreferencesDataStore.setReaderReadingMode(ReadingMode.VERTICAL) }
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
        every { userPreferencesDataStore.observeAutoDownloadNext() } returns flowOf(false)
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun reachedTransitionPage_withAutoDownloadNext_enqueuesNextChapter() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoDownloadNext = true)
            coEvery { downloadedChapterCache.isChapterDownloaded(NEXT_CHAPTER_ID) } returns false
            coEvery { offlineDownloadRepository.getQueueByChapter(NEXT_CHAPTER_ID) } returns null
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            // Assert label chương kế được forward (không chỉ any()) để bắt regression nếu drop nhãn.
            coVerify(exactly = 1) {
                offlineDownloadManager.enqueueDownload(
                    MANGA_ID,
                    NEXT_CHAPTER_ID,
                    mangaTitle = null,
                    chapterLabel = NEXT_CHAPTER.buildTitle(),
                )
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun reachedTransitionPage_withoutAutoDownloadNext_doesNotEnqueue() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoDownloadNext = false)
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                offlineDownloadManager.enqueueDownload(any(), any(), any(), any())
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun autoDownloadNext_skipsWhenChapterAlreadyDownloaded() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoDownloadNext = true)
            coEvery { downloadedChapterCache.isChapterDownloaded(NEXT_CHAPTER_ID) } returns true
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                offlineDownloadManager.enqueueDownload(any(), any(), any(), any())
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun autoDownloadNext_enqueuesOnlyOnceAcrossRepeatedTriggers() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs(autoDownloadNext = true)
            coEvery { downloadedChapterCache.isChapterDownloaded(NEXT_CHAPTER_ID) } returns false
            coEvery { offlineDownloadRepository.getQueueByChapter(NEXT_CHAPTER_ID) } returns null
            val viewModel = createViewModel(startPageIndex = 7, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()
            viewModel.onEvent(ReaderEvent.ReachedTransitionPage)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                offlineDownloadManager.enqueueDownload(MANGA_ID, NEXT_CHAPTER_ID, any(), any())
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun skipReadOn_resolvesNextChapterToNextUnread() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs()
            coEvery { userPreferencesDataStore.getSkipReadChapters() } returns true
            coEvery { chapterDao.getNextUnreadChapter(MANGA_ID, CHAPTER_ID) } returns
                AdjacentChapter(chapterId = "unread-9", chapterNumber = "9", volume = null)
            val viewModel = createViewModel(startPageIndex = 0, nextChapter = NEXT_CHAPTER)
            advanceUntilIdle()

            // Skip-read bật → nextChapterId = chương chưa đọc (không phải chương feed liền kề).
            assertEquals("unread-9", viewModel.state.value.nextChapterId)
            coVerify { chapterDao.getNextUnreadChapter(MANGA_ID, CHAPTER_ID) }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun chapterGap_emitsShowGapWarning() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs()
            val viewModel = createViewModel(startPageIndex = 0)
            // Stub SAU createViewModel để thắng default any()->null của getPrevChapter.
            coEvery { chapterDao.getPrevChapter(MANGA_ID, CHAPTER_ID) } returns
                AdjacentChapter(chapterId = "prev-5", chapterNumber = "5", volume = null)
            coEvery { chapterDao.getChapterNumber(CHAPTER_ID) } returns "8"
            // UNDISPATCHED → collector subscribe đồng bộ NGAY (trước advanceUntilIdle chạy init+emit).
            val gap = async(start = CoroutineStart.UNDISPATCHED) {
                viewModel.effects.first { it is ReaderUiEffect.ShowGapWarning } as ReaderUiEffect.ShowGapWarning
            }
            advanceUntilIdle()

            // Chương hiện tại Ch.8, liền trước Ch.5 → thiếu Ch.6–7.
            assertEquals(6, gap.await().gapStart)
            assertEquals(7, gap.await().gapEnd)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun completedChapter_withDeleteAfterRead_deletesOldKeepingN() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs()
            coEvery { userPreferencesDataStore.getDeleteAfterRead() } returns true
            coEvery { userPreferencesDataStore.getDeleteAfterReadKeep() } returns 2
            coEvery { chapterDao.getCompletedChapterIdsOrdered(MANGA_ID) } returns
                listOf("c1", "c2", "c3", "c4")
            every { downloadedChapterCache.downloadedChapterIds } returns
                MutableStateFlow(setOf("c1", "c2", "c3", "c4"))
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            // 8 trang → index 7 = trang cuối = chương hoàn thành.
            viewModel.onEvent(ReaderEvent.VisiblePageChanged(7))
            advanceUntilIdle()

            // Giữ 2 mới nhất (c3, c4); xóa c1, c2.
            coVerify { offlineDownloadManager.deleteDownload(MANGA_ID, "c1") }
            coVerify { offlineDownloadManager.deleteDownload(MANGA_ID, "c2") }
            coVerify(exactly = 0) { offlineDownloadManager.deleteDownload(MANGA_ID, "c3") }
            coVerify(exactly = 0) { offlineDownloadManager.deleteDownload(MANGA_ID, "c4") }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun completedChapter_neverDeletesCurrentChapter_evenIfAmongOldest() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs()
            coEvery { userPreferencesDataStore.getDeleteAfterRead() } returns true
            coEvery { userPreferencesDataStore.getDeleteAfterReadKeep() } returns 2
            // Chương hiện tại (CHAPTER_ID) nằm trong nhóm cũ nhất → vẫn KHÔNG được xóa.
            coEvery { chapterDao.getCompletedChapterIdsOrdered(MANGA_ID) } returns
                listOf(CHAPTER_ID, "c2", "c3", "c4")
            every { downloadedChapterCache.downloadedChapterIds } returns
                MutableStateFlow(setOf(CHAPTER_ID, "c2", "c3", "c4"))
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.VisiblePageChanged(7))
            advanceUntilIdle()

            // dropLast(2) = [CHAPTER_ID, c2]; chừa chương hiện tại → chỉ xóa c2.
            coVerify(exactly = 0) { offlineDownloadManager.deleteDownload(MANGA_ID, CHAPTER_ID) }
            coVerify { offlineDownloadManager.deleteDownload(MANGA_ID, "c2") }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun completedChapter_withoutDeleteAfterRead_deletesNothing() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubReaderPrefs()
            coEvery { userPreferencesDataStore.getDeleteAfterRead() } returns false
            val viewModel = createViewModel(startPageIndex = 0)
            advanceUntilIdle()

            viewModel.onEvent(ReaderEvent.VisiblePageChanged(7))
            advanceUntilIdle()

            coVerify(exactly = 0) { offlineDownloadManager.deleteDownload(any(), any()) }
        }

    private fun stubReaderPrefs(autoAdvance: Boolean = false, autoDownloadNext: Boolean = false) {
        every { userPreferencesDataStore.observeReaderKeepScreenOn() } returns flowOf(false)
        every { userPreferencesDataStore.observeReaderVolumeKeyNav() } returns flowOf(false)
        every { userPreferencesDataStore.observeReaderBrightness() } returns flowOf(1.0f)
        every { userPreferencesDataStore.observeReaderBackground() } returns flowOf("BLACK")
        every { userPreferencesDataStore.observeReaderAutoAdvance() } returns flowOf(autoAdvance)
        every { userPreferencesDataStore.observeAutoDownloadNext() } returns flowOf(autoDownloadNext)
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
        loadReaderPagesUseCase: LoadReaderPagesUseCase = mockk(),
        stubStoredReadingMode: Boolean = true,
        readerPages: List<String> = DEFAULT_READER_PAGES,
    ): ReaderViewModel {
        val syncReadingProgressUseCase = mockk<SyncReadingProgressUseCase>(relaxed = true)
        val imageLoader = mockk<ImageLoader>(relaxed = true)
        if (stubStoredReadingMode) {
            coEvery { userPreferencesDataStore.getReaderReadingMode() } returns storedReadingMode
        }
        // Mặc định không có override per-manga; test nào cần override stub lại SAU createViewModel.
        coEvery { userPreferencesDataStore.getReaderModeForManga(any()) } returns null
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns
            Result.success(readerPages)
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
            offlineDownloadManager = offlineDownloadManager,
            offlineDownloadRepository = offlineDownloadRepository,
            downloadedChapterCache = downloadedChapterCache,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
        const val NEXT_CHAPTER_ID = "chapter-2"
        val DEFAULT_READER_PAGES = List(8) { "page-${it + 1}" }
        val NEXT_CHAPTER = AdjacentChapter(chapterId = NEXT_CHAPTER_ID, chapterNumber = "2", volume = null)
    }
}
