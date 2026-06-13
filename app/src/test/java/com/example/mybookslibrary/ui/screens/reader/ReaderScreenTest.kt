package com.example.mybookslibrary.ui.screens.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.example.mybookslibrary.data.local.UserPreferencesDataStore
import com.example.mybookslibrary.domain.model.ReadingMode
import com.example.mybookslibrary.domain.usecase.LoadReaderPagesUseCase
import com.example.mybookslibrary.domain.usecase.SyncReadingProgressUseCase
import com.example.mybookslibrary.domain.usecase.TapZoneEvaluator
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.ReaderPageFileBuilder
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

    private val loadReaderPagesUseCase = mockk<LoadReaderPagesUseCase>()
    private val syncReadingProgressUseCase = mockk<SyncReadingProgressUseCase>(relaxed = true)
    private val chapterDao = mockk<com.example.mybookslibrary.data.local.dao.ChapterDao>(relaxed = true)
    private val userPreferencesDataStore = mockk<UserPreferencesDataStore>(relaxed = true)

    private fun viewModel(chapterId: String = "c1"): ReaderViewModel {
        coEvery { userPreferencesDataStore.getReaderReadingMode() } returns ReadingMode.LTR
        coEvery { loadReaderPagesUseCase("m1", chapterId) } returns
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
            loadReaderPagesUseCase = loadReaderPagesUseCase,
            syncReadingProgressUseCase = syncReadingProgressUseCase,
            chapterDao = chapterDao,
            tapZoneEvaluator = TapZoneEvaluator(),
            pageFileBuilder = ReaderPageFileBuilder(),
            userPreferencesDataStore = userPreferencesDataStore,
            imageLoader = mockk(relaxed = true),
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
                loadReaderPagesUseCase = loadReaderPagesUseCase,
                syncReadingProgressUseCase = syncReadingProgressUseCase,
                chapterDao = chapterDao,
                tapZoneEvaluator = TapZoneEvaluator(),
                pageFileBuilder = ReaderPageFileBuilder(),
                userPreferencesDataStore = userPreferencesDataStore,
                imageLoader = mockk(relaxed = true),
                ioDispatcher = UnconfinedTestDispatcher(),
            )

        composeRule.setContent {
            ReaderScreen(onBackClick = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun readerScreen_networkError_doesNotCrash() {
        coEvery { loadReaderPagesUseCase("m1", "c2") } returns
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
                loadReaderPagesUseCase = loadReaderPagesUseCase,
                syncReadingProgressUseCase = syncReadingProgressUseCase,
                chapterDao = chapterDao,
                tapZoneEvaluator = TapZoneEvaluator(),
                pageFileBuilder = ReaderPageFileBuilder(),
                userPreferencesDataStore = userPreferencesDataStore,
                imageLoader = mockk(relaxed = true),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        composeRule.setContent {
            ReaderScreen(onBackClick = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
    }
}
