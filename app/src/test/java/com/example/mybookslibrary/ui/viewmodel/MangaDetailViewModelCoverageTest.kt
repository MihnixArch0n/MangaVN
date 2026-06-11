package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.usecase.ChapterListResult
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.usecase.GetChapterListWithProgressUseCase
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phủ các nhánh của [MangaDetailViewModel] mà test gốc bỏ qua: load detail, observe chapters
 * + tải trang chapter đầu, toggle/ensure library, mark completed/unread, và nhánh nuốt lỗi
 * của launchSafe.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MangaDetailViewModelCoverageTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val manga = mockk<MangaRepository>()
    private val library = mockk<LibraryRepository>(relaxed = true)
    private val useCase = mockk<GetChapterListWithProgressUseCase>()
    private val downloadManager = mockk<OfflineDownloadManager>(relaxed = true)
    private val userPreferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    // Default an toàn cho init (loadMangaDetail + observeChapters + checkLibraryStatus).
    private fun stubInitDefaults(inLibrary: Boolean = false) {
        coEvery { manga.getMangaDetail(MANGA_ID) } returns Result.failure(IllegalStateException("x"))
        every { useCase(MANGA_ID) } returns flowOf(ChapterListResult(emptyList(), emptyList(), ""))
        every { userPreferencesDataStore.observePreferredChapterLanguage() } returns flowOf("")
        coEvery { library.getLibraryItem(MANGA_ID) } returns
            if (inLibrary) mockk(relaxed = true) else null
    }

    private fun build() =
        MangaDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("mangaId" to MANGA_ID)),
            mangaRepository = manga,
            libraryRepository = library,
            getChapterListWithProgressUseCase = useCase,
            offlineDownloadManager = downloadManager,
            userPreferencesDataStore = userPreferencesDataStore,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )

    @Test
    fun loadMangaDetail_thanhCong_capNhatDetail() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            val model = MangaModel(MANGA_ID, "Title", "Desc", null, listOf("tag"))
            coEvery { manga.getMangaDetail(MANGA_ID) } returns Result.success(model)

            val vm = build()
            advanceUntilIdle()

            assertEquals(model, vm.uiState.value.mangaDetail)
        }

    @Test
    fun observeChapters_firstLoad_taiTrangChapterDau() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            every { useCase(MANGA_ID) } returns flowOf(ChapterListResult(listOf(chapter("c1")), emptyList(), ""))
            coEvery { manga.getChapterPages("c1") } returns Result.success(List(8) { "p$it" })

            val vm = build()
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.chapters.size)
            assertEquals(5, vm.uiState.value.firstChapterPages.size) // take(5)
            assertFalse(vm.uiState.value.isLoadingFirstChapterPages)
        }

    @Test
    fun loadFirstChapterPages_thatBai_datError() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            every { useCase(MANGA_ID) } returns flowOf(ChapterListResult(listOf(chapter("c1")), emptyList(), ""))
            coEvery { manga.getChapterPages("c1") } returns Result.failure(IllegalStateException("lỗi trang"))

            val vm = build()
            advanceUntilIdle()

            assertEquals("lỗi trang", vm.uiState.value.firstChapterPagesError)
        }

    @Test
    fun observeChapters_flowNem_datChaptersError() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            every { useCase(MANGA_ID) } returns flow { throw IllegalStateException("use-case lỗi") }

            val vm = build()
            advanceUntilIdle()

            assertEquals("use-case lỗi", vm.uiState.value.chaptersError)
            assertFalse(vm.uiState.value.isLoadingChapters)
        }

    @Test
    fun ensureInLibrary_chuaCo_themVaoLibrary() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults(inLibrary = false)
            val vm = build()
            advanceUntilIdle()

            vm.ensureInLibrary("Title", "cover")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isInLibrary)
            coVerify { library.addToLibrary(mangaId = MANGA_ID, title = "Title", coverUrl = "cover") }
        }

    @Test
    fun ensureInLibrary_daCo_boQua() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults(inLibrary = true)
            val vm = build()
            advanceUntilIdle()

            vm.ensureInLibrary("Title", "cover")
            advanceUntilIdle()

            coVerify(exactly = 0) { library.addToLibrary(any(), any(), any()) }
        }

    @Test
    fun toggleLibrary_themRoiXoa() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults(inLibrary = false)
            val vm = build()
            advanceUntilIdle()

            vm.toggleLibrary("Title", "cover")
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isInLibrary)
            coVerify { library.addToLibrary(mangaId = MANGA_ID, title = "Title", coverUrl = "cover") }

            vm.toggleLibrary("Title", "cover")
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isInLibrary)
            coVerify { library.removeFromLibrary(MANGA_ID) }
        }

    @Test
    fun markChapterCompleted_goiRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            val vm = build()
            advanceUntilIdle()

            vm.markChapterCompleted("c1", 20)
            advanceUntilIdle()

            coVerify { library.markChapterCompleted(MANGA_ID, "c1", 20) }
        }

    @Test
    fun markChapterUnread_goiRepository() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            val vm = build()
            advanceUntilIdle()

            vm.markChapterUnread("c1", 20)
            advanceUntilIdle()

            coVerify { library.markChapterUnread(MANGA_ID, "c1", 20) }
        }

    @Test
    fun launchSafe_nuotException_khongCrash() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            coEvery { library.markChapterCompleted(any(), any(), any()) } throws RuntimeException("db lỗi")
            val vm = build()
            advanceUntilIdle()

            // launchSafe bắt Exception -> không ném ra ngoài
            vm.markChapterCompleted("c1", 20)
            advanceUntilIdle()
        }

    @Test
    fun startChapterDownload_blankChapterId_returnSom() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            val vm = build()
            advanceUntilIdle()

            // chapterId blank -> nhánh phải của `||` -> return
            vm.startChapterDownload("")
            advanceUntilIdle()

            coVerify(exactly = 0) { downloadManager.enqueueDownload(any(), any()) }
        }

    @Test
    fun deleteChapterDownload_blankChapterId_returnSom() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            val vm = build()
            advanceUntilIdle()

            // chapterId blank -> return ngay
            vm.deleteChapterDownload("")
            advanceUntilIdle()

            coVerify(exactly = 0) { downloadManager.deleteDownload(any(), any()) }
        }

    @Test
    fun blankMangaId_guardsReturnEarly_khongGoiRepo() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            // SavedStateHandle rỗng -> mangaId="" -> mọi guard isBlank return sớm
            val vm =
                MangaDetailViewModel(
                    savedStateHandle = SavedStateHandle(mapOf("mangaId" to "")),
                    mangaRepository = manga,
                    libraryRepository = library,
                    getChapterListWithProgressUseCase = useCase,
                    offlineDownloadManager = downloadManager,
                    userPreferencesDataStore = userPreferencesDataStore,
                    ioDispatcher = mainDispatcherRule.dispatcher,
                )
            advanceUntilIdle()
            // các method có guard mangaId/chapterId blank
            vm.startChapterDownload("c1") // mangaId blank -> return
            vm.cancelChapterDownload("") // chapterId blank -> return
            vm.deleteChapterDownload("c1") // mangaId blank -> return
            advanceUntilIdle()

            coVerify(exactly = 0) { manga.getMangaDetail(any()) }
            coVerify(exactly = 0) { downloadManager.enqueueDownload(any(), any()) }
            coVerify(exactly = 0) { downloadManager.deleteDownload(any(), any()) }
        }

    @Test
    fun observeChapters_emissionThuHai_khongReloadFirstChapter() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            stubInitDefaults()
            // 2 lần emit non-empty: lần 1 isFirstLoad=true (tải trang), lần 2 isFirstLoad=false
            every { useCase(MANGA_ID) } returns
                flowOf(ChapterListResult(
                    listOf(chapter("c1")),
                    emptyList(), ""),
                    ChapterListResult(listOf(chapter("c1"), chapter("c2")), emptyList(), "")
                )
            coEvery { manga.getChapterPages("c1") } returns Result.success(listOf("p0"))

            val vm = build()
            advanceUntilIdle()

            assertEquals(2, vm.uiState.value.chapters.size)
            // getChapterPages chỉ gọi 1 lần (chỉ emission đầu trigger load)
            coVerify(exactly = 1) { manga.getChapterPages("c1") }
        }

    private fun chapter(id: String) =
        ChapterWithProgressModel(
            chapterId = id,
            mangaId = MANGA_ID,
            volume = null,
            chapterNumber = "1",
            title = null,
            status = ChapterReadingStatus.UNREAD,
            lastReadPage = 0,
            totalPages = 10,
        )

    private companion object {
        const val MANGA_ID = "manga-1"
    }
}
