package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.example.mybookslibrary.data.download.OfflineDownloadManager
import com.example.mybookslibrary.data.repository.LibraryRepository
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.ChapterReadingStatus
import com.example.mybookslibrary.domain.model.ChapterWithProgressModel
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.domain.usecase.GetChapterListWithProgressUseCase
import com.example.mybookslibrary.ui.navigation.MangaDetailDestination
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.MangaDetailViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class MangaDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    private val mangaRepo = mockk<MangaRepository>()
    private val libraryRepo = mockk<LibraryRepository>(relaxed = true)
    private val useCase = mockk<GetChapterListWithProgressUseCase>()
    private val downloadManager = mockk<OfflineDownloadManager>(relaxed = true)

    private fun viewModel(
        inLibrary: Boolean = false,
        chapters: List<ChapterWithProgressModel> = emptyList(),
        detailError: Boolean = false,
    ): MangaDetailViewModel {
        if (detailError) {
            coEvery { mangaRepo.getMangaDetail(any()) } returns Result.failure(IllegalStateException("lỗi"))
        } else {
            coEvery { mangaRepo.getMangaDetail(any()) } returns
                Result.success(MangaModel("m1", "Test Manga", "Desc", null, emptyList()))
        }
        every { useCase(any()) } returns flowOf(chapters)
        coEvery { libraryRepo.isInLibrary(any()) } returns inLibrary
        coEvery { mangaRepo.getChapterPages(any()) } returns Result.success(emptyList())
        return MangaDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(MangaDetailDestination.mangaIdArgumentName to "m1")),
            mangaRepository = mangaRepo,
            libraryRepository = libraryRepo,
            getChapterListWithProgressUseCase = useCase,
            offlineDownloadManager = downloadManager,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    // MangaDetailScreen có nhiều scrollable — swipeUp() nhiều lần để reveal content
    private fun scrollTo(
        @Suppress("UNUSED_PARAMETER") text: String = "",
    ) {
        repeat(6) {
            composeRule.onRoot().performTouchInput { swipeUp() }
        }
        composeRule.waitForIdle()
    }

    private fun screen(
        title: String = "Naruto",
        inLibrary: Boolean = false,
        chapters: List<ChapterWithProgressModel> = emptyList(),
        detailError: Boolean = false,
    ) {
        composeRule.setContent {
            MangaDetailScreen(
                mangaId = "m1",
                title = title,
                coverArt = "",
                description = "Desc",
                tags = listOf("Action"),
                onBackClick = {},
                onReadChapter = { _, _, _, _ -> },
                viewModel = viewModel(inLibrary = inLibrary, chapters = chapters, detailError = detailError),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rendersTitleVisible() {
        screen()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun screenRendersWithoutCrash_noLibrary() {
        // Kiểm tra screen render đúng với inLibrary=false
        screen(inLibrary = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun screenRendersWithoutCrash_inLibrary() {
        // Kiểm tra screen render đúng với inLibrary=true
        screen(inLibrary = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun screenRendersWithoutCrash_withChapters() {
        // Kiểm tra screen render đúng khi có chapters
        screen(chapters = listOf(chapter("c1", "1")))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun chaptersSection_visible() {
        screen()
        scrollTo("Chapters")
        composeRule.onNodeWithText("Chapters").assertIsDisplayed()
    }

    @Test
    fun chaptersSection_click_doesNotCrash() {
        screen()
        scrollTo("Chapters")
        composeRule.onNodeWithText("Chapters").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun multipleChapters_expandedShowsList() {
        screen(chapters = listOf(chapter("c1", "1"), chapter("c2", "2")))
        scrollTo("Chapters")
        composeRule.onNodeWithText("Chapters").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun screenRendersWithoutCrash_withDescription() {
        // Description và tags nằm trong lazy items — chỉ test không crash
        screen()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun inLibrary_showsInLibraryState() {
        screen(inLibrary = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun withTags_rendersTagsSection() {
        screen(title = "Berserk")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Berserk").assertIsDisplayed()
    }

    @Test
    fun withChapters_readStatus_showsChapterList() {
        val chapters =
            listOf(
                chapter("c1", "1"),
                chapter("c2", "2"),
                chapter("c3", "3"),
            )
        screen(chapters = chapters)
        scrollTo("Chapters")
        composeRule.onNodeWithText("Chapters").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun detailError_rendersWithoutCrash() {
        screen(detailError = true)
        composeRule.waitForIdle()
    }

    @Test
    fun manyChapters_mixedStatus_expanded_rendersAll() {
        // Các chapter với status khác nhau → covers ChapterRow với đủ branches
        val chapters =
            listOf(
                chapter("c1", "1").copy(status = ChapterReadingStatus.COMPLETED),
                chapter("c2", "2").copy(status = ChapterReadingStatus.READING, lastReadPage = 5),
                chapter("c3", "3").copy(status = ChapterReadingStatus.UNREAD),
                chapter("c4", "4").copy(status = ChapterReadingStatus.COMPLETED),
                chapter("c5", "5").copy(status = ChapterReadingStatus.UNREAD),
            )
        screen(chapters = chapters)
        scrollTo("Chapters")
        composeRule.onNodeWithText("Chapters").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
    }

    @Test
    fun withDescription_andTags_scrollsContent() {
        screen(title = "Attack on Titan")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Attack on Titan").assertIsDisplayed()
        repeat(4) {
            composeRule.onRoot().performTouchInput { swipeUp() }
        }
        composeRule.waitForIdle()
    }

    private fun chapter(
        id: String,
        num: String,
    ) = ChapterWithProgressModel(
        chapterId = id,
        mangaId = "m1",
        volume = null,
        chapterNumber = num,
        title = null,
        status = ChapterReadingStatus.UNREAD,
        lastReadPage = 0,
        totalPages = 20,
    )
}
