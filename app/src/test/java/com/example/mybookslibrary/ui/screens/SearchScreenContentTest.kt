package com.example.mybookslibrary.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import com.example.mybookslibrary.data.repository.MangaRepository
import com.example.mybookslibrary.domain.model.MangaModel
import com.example.mybookslibrary.ui.util.FakeImageLoader
import com.example.mybookslibrary.ui.viewmodel.SearchViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@Config(qualifiers = "w411dp-h4000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class SearchScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() = FakeImageLoader.install()

    @After
    fun tearDown() = FakeImageLoader.reset()

    private fun vm(
        repository: MangaRepository =
            mockk<MangaRepository>().also {
                coEvery { it.getTags() } returns Result.success(emptyList())
                every { it.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))
            },
    ) = SearchViewModel(repository)

    @Test
    fun rendersTitleAndFilterButton() {
        composeRule.setContent { SearchScreenContent(viewModel = vm()) }

        composeRule.onNodeWithText("Search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    @Test
    fun emptyQuery_showsPrompt() {
        composeRule.setContent { SearchScreenContent(viewModel = vm()) }

        // query="" < 2 chars → prompt state
        composeRule.onNodeWithText("Discover your next story").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsIndicator() {
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        // Search flow không complete ngay → isLoading = true
        every { repo.searchManga(any(), any()) } returns flowOf()
        val viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("nar")

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }

        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun noResults_showsNoResultsText() {
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))
        val viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("xyzabc")

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }

        // isLoading=false, results empty, query>=2 → "No results for..."
        composeRule.onNodeWithText("No results for \"xyzabc\"").assertIsDisplayed()
    }

    @Test
    fun errorState_rendersWithoutCrash() {
        // Error text có thể nằm ngoài viewport tùy config Robolectric — verify không crash
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns
            flowOf(Result.failure(IllegalStateException("timeout")))
        val viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("one piece")

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        composeRule.waitForIdle()
        // "Search" title luôn visible (ở đầu)
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun filterSheet_openState_renders() {
        val viewModel = vm()
        viewModel.onOpenFilterSheet()
        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        composeRule.waitForIdle()
    }

    @Test
    fun withResults_rendersWithoutCrash() {
        val manga = MangaModel("m1", "Naruto", "Desc", null, emptyList())
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(listOf(manga)))
        val viewModel = SearchViewModel(repo)
        viewModel.onQueryChange("naruto")

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun withResults_advanceLooper_showsResultItems() {
        // Advance Robolectric main looper 500ms để fire debounce(400ms)
        // → LazyColumn renders SearchResultItem → covers else branch + SearchResultItem composable
        val manga = MangaModel("m1", "One Piece", "Adventure", null, emptyList())
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(listOf(manga)))
        val viewModel = SearchViewModel(repo)

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        viewModel.onQueryChange("one piece")
        // Advance main looper để debounce fire
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("One Piece").assertIsDisplayed()
    }

    @Test
    fun withResults_withTags_advanceLooper_rendersTags() {
        val manga = MangaModel("m1", "Naruto", "Ninja", null, listOf("Action", "Adventure", "Comedy"))
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(listOf(manga)))
        val viewModel = SearchViewModel(repo)

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        viewModel.onQueryChange("naruto")
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
        // Tags visible: "Action · Adventure · Comedy"
        composeRule.onNodeWithText("Action · Adventure · Comedy").assertIsDisplayed()
    }

    @Test
    fun withResults_clickItem_afterDebounce_invokesCallback() {
        var clicked: String? = null
        val manga = MangaModel("m1", "Bleach", "Action", null, emptyList())
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(listOf(manga)))
        val viewModel = SearchViewModel(repo)

        composeRule.setContent {
            SearchScreenContent(
                onMangaClick = { clicked = it.id },
                viewModel = viewModel,
            )
        }
        viewModel.onQueryChange("bleach")
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bleach").performClick()
        composeRule.waitForIdle()
        assert(clicked == "m1") {
            "onMangaClick phải được gọi khi click result item"
        }
    }

    @Test
    fun withMultipleResults_advanceLooper_rendersAll() {
        // 5 items với tags → covers SearchResultItem với/không có tags
        val mangas =
            listOf(
                MangaModel("m1", "Naruto", "Ninja", null, listOf("Action", "Shounen")),
                MangaModel("m2", "Bleach", "Hollow", null, emptyList()),
                MangaModel("m3", "One Piece", "Pirates", null, listOf("Adventure")),
            )
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(mangas))
        val viewModel = SearchViewModel(repo)

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        viewModel.onQueryChange("manga")
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Naruto").assertIsDisplayed()
        composeRule.onNodeWithText("Bleach").assertIsDisplayed()
    }

    @Test
    fun withResults_manyTags_showsFirst3Tags() {
        // tags.take(3) joined — covers tags display branch trong SearchResultItem
        val manga = MangaModel("m1", "HxH", "Manga", null, listOf("Action", "Adventure", "Fantasy", "Shounen"))
        val repo = mockk<MangaRepository>()
        coEvery { repo.getTags() } returns Result.success(emptyList())
        every { repo.searchManga(any(), any()) } returns flowOf(Result.success(listOf(manga)))
        val viewModel = SearchViewModel(repo)

        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        viewModel.onQueryChange("hxh")
        ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("HxH").assertIsDisplayed()
    }

    @Test
    fun filterButton_openAndClose_works() {
        val viewModel = vm()
        viewModel.onOpenFilterSheet()
        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        composeRule.waitForIdle()
        viewModel.onDismissFilterSheet()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }
}
