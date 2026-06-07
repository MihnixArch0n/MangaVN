package com.example.mybookslibrary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.usecase.GetChapterListWithProgressUseCase
import com.example.mybookslibrary.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MangaDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startChapterDownload_delegatesToOfflineDownloadManager() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manager = mockManager()
            val viewModel = createViewModel(manager)
            advanceUntilIdle()

            viewModel.startChapterDownload(CHAPTER_ID)
            advanceUntilIdle()

            coVerify(exactly = 1) { manager.enqueueDownload(MANGA_ID, CHAPTER_ID) }
        }

    @Test
    fun cancelChapterDownload_delegatesToOfflineDownloadManager() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manager = mockManager()
            val viewModel = createViewModel(manager)
            advanceUntilIdle()

            viewModel.cancelChapterDownload(CHAPTER_ID)
            advanceUntilIdle()

            coVerify(exactly = 1) { manager.cancelDownload(CHAPTER_ID) }
        }

    @Test
    fun deleteChapterDownload_delegatesToOfflineDownloadManager() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val manager = mockManager()
            val viewModel = createViewModel(manager)
            advanceUntilIdle()

            viewModel.deleteChapterDownload(CHAPTER_ID)
            advanceUntilIdle()

            coVerify(exactly = 1) { manager.deleteDownload(MANGA_ID, CHAPTER_ID) }
        }

    private fun createViewModel(manager: OfflineDownloadManager): MangaDetailViewModel {
        val mangaRepository = mockk<MangaRepository>()
        val libraryRepository = mockk<LibraryRepository>()
        val getChapterListWithProgressUseCase = mockk<GetChapterListWithProgressUseCase>()

        coEvery { mangaRepository.getMangaDetail(MANGA_ID) } returns Result.failure(IllegalStateException("not needed"))
        every { getChapterListWithProgressUseCase(MANGA_ID) } returns flowOf(emptyList())
        coEvery { libraryRepository.isInLibrary(MANGA_ID) } returns false

        return MangaDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("mangaId" to MANGA_ID)),
            mangaRepository = mangaRepository,
            libraryRepository = libraryRepository,
            getChapterListWithProgressUseCase = getChapterListWithProgressUseCase,
            offlineDownloadManager = manager,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    private fun mockManager(): OfflineDownloadManager {
        val manager = mockk<OfflineDownloadManager>()
        coEvery { manager.enqueueDownload(any(), any()) } just Runs
        coEvery { manager.cancelDownload(any()) } just Runs
        coEvery { manager.deleteDownload(any(), any()) } just Runs
        return manager
    }

    private companion object {
        const val MANGA_ID = "manga-1"
        const val CHAPTER_ID = "chapter-1"
    }
}
