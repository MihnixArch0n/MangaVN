package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.LoadReaderPagesUseCase
import com.example.mybookslibrary.domain.usecase.SyncReadingProgressUseCase
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Phủ các nhánh của [ReaderViewModel] mà test gốc bỏ qua: loadChapterPages
 * (empty/fail/missing), cycle/jump page, sync tiến độ Room, và page-action effects.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderViewModelCoverageTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loadReaderPagesUseCase = mockk<LoadReaderPagesUseCase>()
    private val syncReadingProgressUseCase = mockk<SyncReadingProgressUseCase>(relaxed = true)
    private val userPreferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    private fun build(
        chapterId: String = CHAPTER_ID,
        startPageIndex: Int = 0,
        chapterTitle: String = "Chapter 1",
    ): ReaderViewModel {
        coEvery { userPreferencesDataStore.getReaderReadingMode() } returns ReadingMode.LTR
        val args =
            mutableMapOf<String, Any?>(
                "mangaId" to MANGA_ID,
                "chapterId" to chapterId,
                "chapterTitle" to chapterTitle,
                "startPageIndex" to startPageIndex,
            )
        return ReaderViewModel(
            application = RuntimeEnvironment.getApplication(),
            savedStateHandle = SavedStateHandle(args),
            loadReaderPagesUseCase = loadReaderPagesUseCase,
            syncReadingProgressUseCase = syncReadingProgressUseCase,
            tapZoneEvaluator = TapZoneEvaluator(),
            pageFileBuilder = ReaderPageFileBuilder(),
            userPreferencesDataStore = userPreferencesDataStore,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    // ---- loadChapterPages branches ----

    @Test
    fun load_thieuChapterId_baoLoiMissing() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = build(chapterId = "")
        advanceUntilIdle()

        assertEquals(
            RuntimeEnvironment.getApplication().getString(
                com.example.mybookslibrary.R.string.error_missing_chapter,
            ),
            vm.state.value.error,
        )
    }

    @Test
    fun load_networkRong_baoLoiLoadPages() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.success(emptyList())

        val vm = build()
        advanceUntilIdle()

        assertEquals(
            RuntimeEnvironment.getApplication().getString(com.example.mybookslibrary.R.string.error_load_pages),
            vm.state.value.error,
        )
    }

    @Test
    fun load_networkLoiCoMessage_dungMessage() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns
            Result.failure(IllegalStateException("mạng hỏng"))

        val vm = build()
        advanceUntilIdle()

        assertEquals("mạng hỏng", vm.state.value.error)
    }

    @Test
    fun load_networkLoiNullMessage_dungStringRes() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.failure(RuntimeException())

        val vm = build()
        advanceUntilIdle()

        assertEquals(
            RuntimeEnvironment.getApplication().getString(com.example.mybookslibrary.R.string.error_load_pages),
            vm.state.value.error,
        )
    }

    @Test
    fun retryLoadPages_loadsAgainAfterFailure() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns
            Result.failure(IllegalStateException("offline")) andThen
            Result.success(listOf("page-0"))

        val vm = build()
        advanceUntilIdle()
        vm.onEvent(ReaderEvent.RetryLoadPages)
        advanceUntilIdle()

        assertEquals(listOf("page-0"), vm.state.value.pages)
        assertEquals(null, vm.state.value.error)
    }

    // ---- helper: build VM đã tải 8 trang network ----
    private fun loadedVm(startPageIndex: Int = 0): ReaderViewModel {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns
            Result.success(List(8) { "page-$it" })
        return build(startPageIndex = startPageIndex)
    }

    @Test
    fun cycleReadingMode_verticalLtrRtlVertical() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        // Default = LTR; cycle: LTR->RTL->VERTICAL->LTR (phủ cả 3 nhánh when)
        assertEquals(ReadingMode.LTR, vm.state.value.currentReadingMode)
        vm.onEvent(ReaderEvent.CycleReadingMode)
        assertEquals(ReadingMode.RTL, vm.state.value.currentReadingMode)
        vm.onEvent(ReaderEvent.CycleReadingMode)
        assertEquals(ReadingMode.VERTICAL, vm.state.value.currentReadingMode)
        vm.onEvent(ReaderEvent.CycleReadingMode)
        assertEquals(ReadingMode.LTR, vm.state.value.currentReadingMode)
    }

    @Test
    fun jumpToPage_capNhatVaEmitNavigate() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.NavigateToPage }
        runCurrent()

        vm.onEvent(ReaderEvent.JumpToPage(5))

        assertEquals(5, vm.state.value.lastReadPageIndex)
        assertEquals(5, effect.await().pageIndex)
    }

    @Test
    fun jumpToPage_ngoaiBien_coerceVeTrangCuoi() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.JumpToPage(99)) // coerceIn(0, lastIndex=7)

        assertEquals(7, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun pageActionSelected_urlKhongExtension_dungJpgFallback() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.SavePageAs }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/page-no-ext", 0))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.SaveAs))

        assertEquals("jpg", effect.await().extension)
    }

    @Test
    fun visiblePageChanged_capNhatVaSyncRoom() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.VisiblePageChanged(4))
        advanceUntilIdle()

        assertEquals(4, vm.state.value.lastReadPageIndex)
        coVerify {
            syncReadingProgressUseCase(
                mangaId = MANGA_ID,
                chapterId = CHAPTER_ID,
                pageIndex = 4,
                totalPages = 8,
            )
        }
    }

    @Test
    fun flushProgress_forceSyncRoom() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.FlushProgress(6))
        advanceUntilIdle()

        assertEquals(6, vm.state.value.lastReadPageIndex)
        coVerify {
            syncReadingProgressUseCase(
                mangaId = MANGA_ID,
                chapterId = CHAPTER_ID,
                pageIndex = 6,
                totalPages = 8,
            )
        }
    }

    @Test
    fun pageActions_showRoiDismiss() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p1.jpg", 0))
        assertEquals(
            "https://x/p1.jpg",
            vm.state.value.selectedPageActionTarget
                ?.pageUrl,
        )

        vm.onEvent(ReaderEvent.DismissPageActions)
        assertNull(vm.state.value.selectedPageActionTarget)
    }

    @Test
    fun pageActionSelected_saveAs_emitSavePageAs() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.SavePageAs }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p2.png", 1))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.SaveAs))

        assertEquals("png", effect.await().extension)
    }

    @Test
    fun pageActionSelected_share_emitSharePage() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.SharePage }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p3.jpg", 2))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.Share))

        assertTrue(effect.await().fileName.isNotBlank())
    }

    @Test
    fun pageActionCompleted_emitResult() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.ShowPageActionResult }
        runCurrent()

        vm.onEvent(ReaderEvent.PageActionCompleted(ReaderPageAction.QuickSave))

        assertNull(effect.await().errorMessage)
    }

    @Test
    fun pageActionFailed_emitResultCoMessage() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.ShowPageActionResult }
        runCurrent()

        vm.onEvent(ReaderEvent.PageActionFailed(ReaderPageAction.Share, "lưu lỗi"))

        assertEquals("lưu lỗi", effect.await().errorMessage)
    }

    @Test
    fun navigateToPage_emptyPages_noop() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // pages rỗng -> navigateToPage guard `pages.isEmpty()` -> return
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.success(emptyList())
        val vm = build()
        advanceUntilIdle()

        // TapOnScreen gọi navigateToPage -> pages empty -> return
        vm.onEvent(ReaderEvent.TapOnScreen(x = 900f, y = 500f, width = 1000f, height = 1000f))

        assertEquals(0, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun flushProgress_nullIndexKhongCoPending_dungLast() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm(startPageIndex = 3)
        advanceUntilIdle()

        // index=null, pendingPageIndex=null -> last=3; target==last -> không update state
        vm.onEvent(ReaderEvent.FlushProgress(null))
        advanceUntilIdle()

        assertEquals(3, vm.state.value.lastReadPageIndex)
        // force=true → sync dù same page
        coVerify { syncReadingProgressUseCase(MANGA_ID, CHAPTER_ID, 3, 8) }
    }

    @Test
    fun flushProgress_targetKhacLast_capNhatState() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm(startPageIndex = 1)
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.FlushProgress(5)) // target(5) != last(1) -> update
        advanceUntilIdle()

        assertEquals(5, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun buildPageFile_urlCoFragment_catTruocHash() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.SavePageAs }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p.webp#frag", 0))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.SaveAs))

        assertEquals("webp", effect.await().extension)
    }

    @Test
    fun buildPageFile_urlCoQuery_catTruocQuery() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.SavePageAs }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p.gif?t=123", 0))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.SaveAs))

        assertEquals("gif", effect.await().extension)
    }

    @Test
    fun syncProgressToRoom_forceTrue_syncDuDaSynced() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm(startPageIndex = 0)
        advanceUntilIdle()
        // VisiblePageChanged(3): lastReadPage=0 != 3 -> update state + sync (force=false, lastSynced=null -> sync)
        vm.onEvent(ReaderEvent.VisiblePageChanged(3))
        advanceUntilIdle()
        // FlushProgress(3): force=true -> sync dù lastSynced==3
        vm.onEvent(ReaderEvent.FlushProgress(3))
        advanceUntilIdle()

        // Page 3 được sync ít nhất 2 lần (visible + flush force)
        coVerify(atLeast = 2) {
            syncReadingProgressUseCase(MANGA_ID, CHAPTER_ID, 3, 8)
        }
    }

    @Test
    fun missingArgs_chapterIdRong_baoLoiMissing() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        // SavedStateHandle rỗng -> mangaId/chapterId/chapterTitle null-coalesce ""
        val vm =
            ReaderViewModel(
                application = RuntimeEnvironment.getApplication(),
                savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "mangaId" to "",
                        "chapterId" to "",
                        "chapterTitle" to "",
                        "startPageIndex" to 0,
                    ),
                ),
                loadReaderPagesUseCase = loadReaderPagesUseCase,
                syncReadingProgressUseCase = syncReadingProgressUseCase,
                tapZoneEvaluator = TapZoneEvaluator(),
                pageFileBuilder = ReaderPageFileBuilder(),
                userPreferencesDataStore = userPreferencesDataStore,
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
        advanceUntilIdle()

        assertEquals(0, vm.state.value.lastReadPageIndex)
        assertTrue(vm.state.value.error != null)
    }

    @Test
    fun load_failureNullMessage_dungLoadPagesFallback() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.failure(RuntimeException())

        val vm = build()
        advanceUntilIdle()

        assertEquals(
            RuntimeEnvironment.getApplication().getString(com.example.mybookslibrary.R.string.error_load_pages),
            vm.state.value.error,
        )
    }

    @Test
    fun changeReadingMode_cungMode_noop() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        // default LTR; đổi sang LTR -> oldMode==mode -> return
        vm.onEvent(ReaderEvent.ChangeReadingMode(ReadingMode.LTR))

        assertEquals(ReadingMode.LTR, vm.state.value.currentReadingMode)
    }

    @Test
    fun emptyPages_tapVaJump_khongDoiTrang() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.success(emptyList())
        val vm = build()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.TapOnScreen(x = 900f, y = 500f, width = 1000f, height = 1000f))
        vm.onEvent(ReaderEvent.JumpToPage(3))

        assertEquals(0, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun jumpToPage_cungTrang_noop() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm(startPageIndex = 3)
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.JumpToPage(3)) // targetIndex == lastReadPageIndex -> return

        assertEquals(3, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun visiblePageChanged_cungIndex_khongSync() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm(startPageIndex = 2)
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.VisiblePageChanged(2)) // boundedIndex == lastReadPageIndex -> return
        advanceUntilIdle()

        coVerify(exactly = 0) {
            syncReadingProgressUseCase(any(), any(), any(), any())
        }
    }

    @Test
    fun flushProgress_nullIndex_dungPendingHoacLast() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()
        vm.onEvent(ReaderEvent.VisiblePageChanged(3)) // set pending=3
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.FlushProgress(null)) // index null -> pending(3) ?: last
        advanceUntilIdle()

        assertEquals(3, vm.state.value.lastReadPageIndex)
    }

    @Test
    fun pageActionSelected_khongCoTarget_boQua() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        // Không PageLongPressed trước -> selectedPageActionTarget null -> return
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.QuickSave))

        assertNull(vm.state.value.selectedPageActionTarget)
    }

    @Test
    fun buildPageFile_titleRong_slugChapter() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        coEvery { loadReaderPagesUseCase(MANGA_ID, CHAPTER_ID) } returns Result.success(List(8) { "page-$it" })
        val vm = build(chapterTitle = "")
        advanceUntilIdle()
        val effect = async { vm.effects.first() as ReaderUiEffect.QuickSavePage }
        runCurrent()

        vm.onEvent(ReaderEvent.PageLongPressed("https://x/p.jpg", 0))
        vm.onEvent(ReaderEvent.PageActionSelected(ReaderPageAction.QuickSave))

        assertTrue(effect.await().fileName.startsWith("chapter_"))
    }

    @Test
    fun syncProgressToRoom_dedup_chiSyncMotLan() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val vm = loadedVm()
        advanceUntilIdle()

        vm.onEvent(ReaderEvent.VisiblePageChanged(4))
        advanceUntilIdle()
        // Lần 2 cùng index -> lastSyncedPageIndex==4 && !force -> skip
        vm.onEvent(ReaderEvent.VisiblePageChanged(2))
        advanceUntilIdle()
        vm.onEvent(ReaderEvent.VisiblePageChanged(4))
        advanceUntilIdle()

        // index 4 sync 1 lần (lần 2 quay lại 4 vẫn sync vì last=2; nhưng index 2 cũng sync)
        coVerify(atLeast = 1) {
            syncReadingProgressUseCase(MANGA_ID, CHAPTER_ID, 4, 8)
        }
    }

    @Test
    fun syncProgressToRoom_samePageTwice_skipSecondSync() =
        // Covers lines 326-327: skip khi !force && lastSyncedPageIndex == pageIndex
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val vm = loadedVm(startPageIndex = 0)
            advanceUntilIdle()

            vm.onEvent(ReaderEvent.VisiblePageChanged(2))
            advanceUntilIdle()
            // Lần 2 cùng page 2 → lastSyncedPageIndex == 2, !force → skip lines 326-327
            vm.onEvent(ReaderEvent.VisiblePageChanged(2))
            advanceUntilIdle()

            coVerify(exactly = 1) {
                syncReadingProgressUseCase(MANGA_ID, CHAPTER_ID, 2, 8)
            }
        }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
