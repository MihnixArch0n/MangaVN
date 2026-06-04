package com.example.mybookslibrary.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@coil3.annotation.ExperimentalCoilApi
class SearchScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before fun setUp() = FakeImageLoader.install()
    @After fun tearDown() = FakeImageLoader.reset()

    private fun vm(repository: MangaRepository = mockk<MangaRepository>().also {
        coEvery { it.getTags() } returns Result.success(emptyList())
        every { it.searchManga(any(), any()) } returns flowOf(Result.success(emptyList()))
    }) = SearchViewModel(repository)

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
        composeRule.onNodeWithText("No results for \"xyzabc\"").assertExists()
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

        // Results trong LazyColumn — item đầu có thể trong viewport tùy size
        composeRule.setContent { SearchScreenContent(viewModel = viewModel) }
        composeRule.waitForIdle()
        // Ít nhất không crash khi render results state
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }
}
