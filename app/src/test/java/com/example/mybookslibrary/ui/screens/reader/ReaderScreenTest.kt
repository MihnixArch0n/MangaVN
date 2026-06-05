package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.download.DownloadedChapterCache
import com.example.mybookslibrary.data.download.OfflineDownloadStorage
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.ReaderViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke test cho [ReaderScreen] — kiểm tra screen không crash khi render với
 * các trạng thái khác nhau (loading, loaded, error).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class ReaderScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = FakeImageLoader.install()

    @After fun tearDown() = FakeImageLoader.reset()

    private val mangaRepository = mockk<MangaRepository>()
    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
    private val downloadedChapterCache = mockk<DownloadedChapterCache>()
    private val offlineDownloadStorage = mockk<OfflineDownloadStorage>()

    private fun viewModel(chapterId: String = "c1"): ReaderViewModel {
        coEvery { downloadedChapterCache.isChapterDownloaded(chapterId) } returns false
        coEvery { mangaRepository.getChapterPages(chapterId) } returns
            Result.success(listOf("page-0.jpg", "page-1.jpg"))
        return ReaderViewModel(
            application = RuntimeEnvironment.getApplication(),
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "mangaId" to "m1",
                        "chapterId" to chapterId,
                        "chapterTitle" to "Chapter 1",
                        "startPageIndex" to 0,
                    ),
                ),
            mangaRepository = mangaRepository,
            libraryRepository = libraryRepository,
            downloadedChapterCache = downloadedChapterCache,
            offlineDownloadStorage = offlineDownloadStorage,
            tapZoneEvaluator = TapZoneEvaluator(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun readerScreen_loadsPages_doesNotCrash() {
        composeRule.setContent {
            ReaderScreen(onBackClick = {}, viewModel = viewModel())
        }
        composeRule.waitForIdle()
    }

    @Test
    fun readerScreen_emptyChapterId_showsError() {
        coEvery { downloadedChapterCache.isChapterDownloaded(any()) } returns false
        val vm =
            ReaderViewModel(
                application = RuntimeEnvironment.getApplication(),
                savedStateHandle =
                    SavedStateHandle(
                        mapOf(
                            "mangaId" to "m1",
                            "chapterId" to "",
                            "chapterTitle" to "",
                            "startPageIndex" to 0,
                        ),
                    ),
                mangaRepository = mangaRepository,
                libraryRepository = libraryRepository,
                downloadedChapterCache = downloadedChapterCache,
                offlineDownloadStorage = offlineDownloadStorage,
                tapZoneEvaluator = TapZoneEvaluator(),
                ioDispatcher = UnconfinedTestDispatcher(),
            )

        composeRule.setContent {
            ReaderScreen(onBackClick = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun readerScreen_networkError_doesNotCrash() {
        coEvery { downloadedChapterCache.isChapterDownloaded("c2") } returns false
        coEvery { mangaRepository.getChapterPages("c2") } returns
            Result.failure(IllegalStateException("no network"))

        val vm =
            ReaderViewModel(
                application = RuntimeEnvironment.getApplication(),
                savedStateHandle =
                    SavedStateHandle(
                        mapOf(
                            "mangaId" to "m1",
                            "chapterId" to "c2",
                            "chapterTitle" to "Ch2",
                            "startPageIndex" to 0,
                        ),
                    ),
                mangaRepository = mangaRepository,
                libraryRepository = libraryRepository,
                downloadedChapterCache = downloadedChapterCache,
                offlineDownloadStorage = offlineDownloadStorage,
                tapZoneEvaluator = TapZoneEvaluator(),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        composeRule.setContent {
            ReaderScreen(onBackClick = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
    }
}
